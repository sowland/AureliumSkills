package dev.aurelium.auraskills.common.storage.mongodb;

import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import dev.aurelium.auraskills.api.ability.AbstractAbility;
import dev.aurelium.auraskills.api.registry.NamespacedId;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.stat.Stat;
import dev.aurelium.auraskills.api.stat.StatModifier;
import dev.aurelium.auraskills.api.trait.Trait;
import dev.aurelium.auraskills.api.trait.TraitModifier;
import dev.aurelium.auraskills.common.AuraSkillsPlugin;
import dev.aurelium.auraskills.common.ability.AbilityData;
import dev.aurelium.auraskills.common.config.Option;
import dev.aurelium.auraskills.common.storage.StorageProvider;
import dev.aurelium.auraskills.common.ui.ActionBarType;
import dev.aurelium.auraskills.common.user.SkillLevelMaps;
import dev.aurelium.auraskills.common.user.User;
import dev.aurelium.auraskills.common.user.UserState;
import dev.aurelium.auraskills.common.util.data.KeyIntPair;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.codecs.UuidCodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.jetbrains.annotations.NotNull;

import java.sql.Types;
import java.util.*;

/**
 * MongoDB storage provider.
 * For sowland.
 */
public class MongoDBStorageProvider extends StorageProvider {
    private final MongoClient mongoClient;
    private final String databaseName;

    public final int STAT_MODIFIER_ID = 1;
    public final int TRAIT_MODIFIER_ID = 2;
    public final int ABILITY_DATA_ID = 3;
    public final int UNCLAIMED_ITEMS_ID = 4;
    public final int ACTION_BAR_ID = 5;

    public MongoDBStorageProvider(AuraSkillsPlugin plugin, String uri, String databaseName) {
        super(plugin);
        plugin.logger().info("Connecting to MongoDB database: " + uri);
        this.mongoClient = MongoClients.create(uri);
        this.databaseName = databaseName;
    }

    private MongoCollection<Document> getCollection(String collectionName) {
        return mongoClient.getDatabase(databaseName).getCollection(collectionName);
    }

    private SkillLevelMaps loadSkillLevels(UUID uuid, int userId) {
        Map<Skill, Integer> levelsMap = new HashMap<>();
        Map<Skill, Double> xpMap = new HashMap<>();

        MongoCollection<Document> collection = getCollection(MongoDBCollectionName.SKILL_LEVELS.getCollectionName());
        List<Document> data = collection.find(Filters.eq("uid", userId)).into(new ArrayList<>());

        for (Document document : data) {
            String skillName = document.getString("skill_name");
            Skill skill = plugin.getSkillRegistry().get(NamespacedId.fromString(skillName));
            int level = document.getInteger("skill_level");
            double xp = document.getDouble("skill_xp");
            levelsMap.put(skill, level);
            xpMap.put(skill, xp);
        }

        return new SkillLevelMaps(levelsMap, xpMap);
    }

    @Override
    protected User loadRaw(UUID uuid) {
        MongoCollection<Document> collection = getCollection(MongoDBCollectionName.SKILL_ACCOUNT.getCollectionName());
        Document data = collection.find(Filters.eq("player_uuid", uuid.toString())).first();
        User user = userManager.createNewUser(uuid);
        if (data == null) { // If the player doesn't exist in the database
            return user;
        }
        int userId = data.getInteger("uid");

        // Load skill levels and xp
        SkillLevelMaps skillLevelMaps = loadSkillLevels(uuid, userId);
        // Apply skill levels and xp from maps
        for (Map.Entry<Skill, Integer> entry : skillLevelMaps.levels().entrySet()) {
            user.setSkillLevel(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<Skill, Double> entry : skillLevelMaps.xp().entrySet()) {
            user.setSkillXp(entry.getKey(), entry.getValue());
        }
        // Load locale
        String localeString = data.getString("locale");
        if (localeString != null) {
            user.setLocale(new Locale(localeString));
        }
        // Load mana
        double mana = data.getDouble("mana");
        user.setMana(mana);
        // Load stat modifiers
        loadStatModifiers(uuid, userId).values().forEach(user::addStatModifier);
        // Load trait modifiers
        loadTraitModifiers(uuid, userId).values().forEach(user::addTraitModifier);
        // Load ability data
        loadAbilityData(user, userId);
        // Load unclaimed items
        user.setUnclaimedItems(loadUnclaimedItems(userId));
        user.clearInvalidItems();
        // Load action bar
        loadActionBar(user, userId);
        return user;
    }

    private Map<String, StatModifier> loadStatModifiers(UUID uuid, int userId) {
        MongoCollection<Document> collection = getCollection(MongoDBCollectionName.KEY_VALUES.getCollectionName());
        List<Document> data = collection.find(Filters.and(Filters.eq("uid", userId), Filters.eq("data_id", STAT_MODIFIER_ID))).into(new ArrayList<>());
        Map<String, StatModifier> modifiers = new HashMap<>();
        for (Document document : data) {
            Stat stat = plugin.getStatRegistry().get(NamespacedId.fromString(document.getString("category_id")));
            String key = document.getString("key_name");
            double value = document.getDouble("value");
            StatModifier modifier = new StatModifier(key, stat, value);
            modifiers.put(key, modifier);
        }
        return modifiers;
    }

    private Map<String, TraitModifier> loadTraitModifiers(UUID uuid, int userId) {
        MongoCollection<Document> collection = getCollection(MongoDBCollectionName.KEY_VALUES.getCollectionName());
        List<Document> data = collection.find(Filters.and(Filters.eq("uid", userId), Filters.eq("data_id", TRAIT_MODIFIER_ID))).into(new ArrayList<>());
        Map<String, TraitModifier> modifiers = new HashMap<>();
        for (Document document : data) {
            String key = document.getString("key_name");
            Trait trait = plugin.getTraitRegistry().get(NamespacedId.fromString(document.getString("category_id")));
            double value = document.getDouble("value");
            TraitModifier modifier = new TraitModifier(key, trait, value);
            modifiers.put(key, modifier);
        }
        return modifiers;
    }

    private void loadAbilityData(User user, int userId) {
        MongoCollection<Document> collection = getCollection(MongoDBCollectionName.KEY_VALUES.getCollectionName());
        List<Document> data = collection.find(Filters.and(Filters.eq("uid", userId), Filters.eq("data_id", ABILITY_DATA_ID))).into(new ArrayList<>());
        Map<AbstractAbility, AbilityData> abilityDataMap = new HashMap<>();
        for (Document document : data) {
            AbstractAbility ability = plugin.getAbilityRegistry().get(NamespacedId.fromString(document.getString("category_id")));

            String keyName = document.getString("key_name");
            String value = document.getString("value");

            Object parsed = value;
            if (value.equals("true")) {
                parsed = true;
            } else if (value.equals("false")) {
                parsed = false;
            } else {
                try {
                    parsed = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    try {
                        parsed = Double.parseDouble(value);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            user.getAbilityData(ability).setData(keyName, parsed);
        }
    }

    private List<KeyIntPair> loadUnclaimedItems(int userId) {
        MongoCollection<Document> collection = getCollection(MongoDBCollectionName.KEY_VALUES.getCollectionName());
        List<Document> data = collection.find(Filters.and(Filters.eq("uid", userId), Filters.eq("data_id", UNCLAIMED_ITEMS_ID))).into(new ArrayList<>());
        List<KeyIntPair> unclaimedItems = new ArrayList<>();
        for (Document document : data) {
            String key = document.getString("key_name");
            int value = document.getInteger("value");
            unclaimedItems.add(new KeyIntPair(key, value));
        }
        return unclaimedItems;
    }

    private void loadActionBar(User user, int userId) {
        MongoCollection<Document> collection = getCollection(MongoDBCollectionName.KEY_VALUES.getCollectionName());
        List<Document> data = collection.find(Filters.and(Filters.eq("uid", userId), Filters.eq("data_id", ACTION_BAR_ID))).into(new ArrayList<>());
        for (Document document : data) {
            String keyName = document.getString("key_name");
            String value = document.getString("value");
            try {
                ActionBarType type = ActionBarType.valueOf(keyName.toUpperCase(Locale.ROOT));
                boolean enabled = !value.equals("false");
                user.setActionBarSetting(type, enabled);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    @Override
    public @NotNull UserState loadState(UUID uuid) {
        MongoCollection<Document> collection = getCollection(MongoDBCollectionName.SKILL_ACCOUNT.getCollectionName());
        Document data = collection.find(Filters.eq("player_uuid", uuid.toString())).first();

        if (data == null) {
            return UserState.createEmpty(uuid, plugin);
        }
        int userId = data.getInteger("uid");
        // Load skill levels and xp
        SkillLevelMaps skillLevelMaps = loadSkillLevels(uuid, userId);
        // Load stat modifiers
        Map<String, StatModifier> statModifiers = loadStatModifiers(uuid, userId);
        // Load trait modifiers
        Map<String, TraitModifier> traitModifiers = loadTraitModifiers(uuid, userId);
        // Load mana
        double mana = data.getDouble("mana");

        return new UserState(uuid, skillLevelMaps.levels(), skillLevelMaps.xp(), statModifiers, traitModifiers, mana);
    }

    @Override
    public void applyState(UserState state) {
        // Insert user account
        MongoCollection<Document> collection = getCollection(MongoDBCollectionName.SKILL_ACCOUNT.getCollectionName());
        Document data = collection.findOneAndUpdate(Filters.eq("player_uuid", state.uuid().toString()), new Document("$set", new Document("mana", state.mana())));
        if (data == null) {
            collection.insertOne(new Document("player_uuid", state.uuid().toString()).append("mana", state.mana()));
        }
        // Insert skill levels
        int userId = getUserId(state.uuid());
        MongoCollection<Document> levels = getCollection(MongoDBCollectionName.SKILL_LEVELS.getCollectionName());
        for (Map.Entry<Skill, Integer> entry : state.skillLevels().entrySet()) {
            Document update = levels.findOneAndUpdate(Filters.and(Filters.eq("uid", userId), Filters.eq("skill_name", entry.getKey().getId().toString())), new Document("$set", new Document("skill_level", entry.getValue()).append("skill_xp", state.skillXp().get(entry.getKey()))));
            if (update == null) {
                levels.insertOne(new Document("uid", userId).append("skill_name", entry.getKey().getId().toString()).append("skill_level", entry.getValue()).append("skill_xp", state.skillXp().get(entry.getKey())));
            }
        }
        // Save stat modifiers
        saveStatModifiers(userId, state.statModifiers());
        // Save trait modifiers
        saveTraitModifiers(userId, state.traitModifiers());
    }
    private static int getNextUserId(MongoCollection<Document> counterCollection) {
        Document filter = new Document("_id", "users");
        Document update = new Document("$inc", new Document("sequence", 1));

        Document result = counterCollection.findOneAndUpdate(filter, update);

        if (result == null) {
            // 如果递增文档不存在，则插入新的递增文档
            counterCollection.insertOne(new Document("_id", "users").append("sequence", 1));
            return 1;
        }

        return result.getInteger("sequence");
    }

    private int getUserId(UUID uuid) {
        MongoCollection<Document> collection = getCollection(MongoDBCollectionName.SKILL_ACCOUNT.getCollectionName());
        Document data = collection.find(Filters.eq("player_uuid", uuid.toString())).first();
        if (data == null) {
            return -1;
        }
        return data.getInteger("uid");
    }

    /**
     * 玩家退出时候进行保存.
     * @param user
     */
    @Override
    public void save(@NotNull User user) {
        if (user.shouldNotSave()) return;

        // Don't save blank profiles if the option is disabled
        if (!plugin.configBoolean(Option.SAVE_BLANK_PROFILES) && user.isBlankProfile()) {
            return;
        }

        // 保存临时账户
        MongoCollection<Document> account = getCollection(MongoDBCollectionName.SKILL_ACCOUNT.getCollectionName());
        Document update = account.findOneAndUpdate(Filters.eq("player_uuid",user.getUuid().toString()),new Document("$set",new Document("locale", user.getLocale().toLanguageTag()).append("mana", user.getMana())));

        if(update == null){
            int newUserId = getNextUserId(getCollection("counters"));
            account.insertOne(new Document("uid", newUserId).append("player_uuid", user.getUuid().toString()).append("locale", user.getLocale().toLanguageTag()).append("mana", user.getMana()));
        }

        int userId = getUserId(user.getUuid());

        // 保存技能等级
        MongoCollection<Document> levels = getCollection(MongoDBCollectionName.SKILL_LEVELS.getCollectionName());
        for (Map.Entry<Skill, Integer> entry : user.getSkillLevelMap().entrySet()) {
            Document skill = levels.findOneAndUpdate(Filters.and(Filters.eq("uid", userId), Filters.eq("skill_name", entry.getKey().getId().toString())), new Document("$set", new Document("skill_level", entry.getValue()).append("skill_xp", user.getSkillXp(entry.getKey()))));
            if(skill == null){
                levels.insertOne(new Document("uid", userId).append("skill_name", entry.getKey().getId().toString()).append("skill_level", entry.getValue()).append("skill_xp", user.getSkillXp(entry.getKey())));
            }
        }

        // 处理KV存储
        // 1. Delete existing key values
        MongoCollection<Document> keyValues = getCollection(MongoDBCollectionName.KEY_VALUES.getCollectionName());
        keyValues.deleteMany(Filters.eq("uid",userId));
        // 2. save key values"uid",
        saveStatModifiers(userId, user.getStatModifiers());
        saveTraitModifiers(userId, user.getTraitModifiers());
        saveAbilityData(userId, user.getAbilityDataMap());
        saveUnclaimedItems(userId, user.getUnclaimedItems());
        saveActionBar(userId, user);
    }

    @Override
    public void delete(UUID uuid) {
        MongoCollection<Document> account = getCollection(MongoDBCollectionName.SKILL_ACCOUNT.getCollectionName());
        account.deleteOne(Filters.eq("player_uuid", uuid.toString()));
        int userId = getUserId(uuid);
        MongoCollection<Document> levels = getCollection(MongoDBCollectionName.SKILL_LEVELS.getCollectionName());
        levels.deleteMany(Filters.eq("uid", userId));
        MongoCollection<Document> keyValues = getCollection(MongoDBCollectionName.KEY_VALUES.getCollectionName());
        keyValues.deleteMany(Filters.eq("uid", userId));
    }

    private void saveStatModifiers(int userId, Map<String, StatModifier> modifiers) {
        if (modifiers.isEmpty()) {
            return;
        }
        plugin.logger().info("Saving stat modifiers for user " + userId);
        MongoCollection<Document> collection = getCollection(MongoDBCollectionName.KEY_VALUES.getCollectionName());
        for (StatModifier modifier : modifiers.values()) {
            Document update = collection.findOneAndUpdate(Filters.and(Filters.eq("uid", userId), Filters.eq("data_id", STAT_MODIFIER_ID), Filters.eq("category_id", modifier.stat().getId().toString()), Filters.eq("key_name", modifier.name())), new Document("$set", new Document("value", modifier.value())));
            if (update == null) {
                collection.insertOne(new Document("uid", userId).append("data_id", STAT_MODIFIER_ID).append("category_id", modifier.stat().getId().toString()).append("key_name", modifier.name()).append("value", modifier.value()));
            }
        }
    }

    private void saveTraitModifiers(int userId, Map<String, TraitModifier> modifiers) {
        if (modifiers.isEmpty()) {
            return;
        }
        MongoCollection<Document> collection = getCollection(MongoDBCollectionName.KEY_VALUES.getCollectionName());
        for (TraitModifier modifier : modifiers.values()) {
            Document update = collection.findOneAndUpdate(Filters.and(Filters.eq("uid", userId), Filters.eq("data_id", TRAIT_MODIFIER_ID), Filters.eq("category_id", modifier.trait().getId().toString()), Filters.eq("key_name", modifier.name())), new Document("$set", new Document("value", modifier.value())));
            if (update == null) {
                collection.insertOne(new Document("uid", userId).append("data_id", TRAIT_MODIFIER_ID).append("category_id", modifier.trait().getId().toString()).append("key_name", modifier.name()).append("value", modifier.value()));
            }
        }
    }

    private void saveAbilityData(int userId, Map<AbstractAbility, AbilityData> abilityDataMap) {
        if (abilityDataMap.isEmpty()) {
            return;
        }
        MongoCollection<Document> collection = getCollection(MongoDBCollectionName.KEY_VALUES.getCollectionName());
        for (AbilityData abilityData : abilityDataMap.values()) {
            for (Map.Entry<String, Object> dataEntry : abilityData.getDataMap().entrySet()) {
                Document update = collection.findOneAndUpdate(Filters.and(Filters.eq("uid", userId), Filters.eq("data_id", ABILITY_DATA_ID), Filters.eq("category_id", abilityData.getAbility().getId().toString()), Filters.eq("key_name", dataEntry.getKey())), new Document("$set", new Document("value", dataEntry.getValue().toString())));
                if (update == null) {
                    collection.insertOne(new Document("uid", userId).append("data_id", ABILITY_DATA_ID).append("category_id", abilityData.getAbility().getId().toString()).append("key_name", dataEntry.getKey()).append("value", dataEntry.getValue().toString()));
                }
            }
        }
    }

    private void saveUnclaimedItems(int userId, List<KeyIntPair> unclaimedItems) {
        if (unclaimedItems.isEmpty()) {
            return;
        }
        MongoCollection<Document> collection = getCollection(MongoDBCollectionName.KEY_VALUES.getCollectionName());
        for (KeyIntPair unclaimedItem : unclaimedItems) {
            Document update = collection.findOneAndUpdate(Filters.and(Filters.eq("uid", userId), Filters.eq("data_id", UNCLAIMED_ITEMS_ID), Filters.eq("category_id", Types.NULL), Filters.eq("key_name", unclaimedItem.getKey())), new Document("$set", new Document("value", unclaimedItem.getValue())));
            if (update == null) {
                collection.insertOne(new Document("uid", userId).append("data_id", UNCLAIMED_ITEMS_ID).append("category_id", Types.NULL).append("key_name", unclaimedItem.getKey()).append("value", unclaimedItem.getValue()));
            }
        }
    }

    private void saveActionBar(int userId, User user) {
        boolean shouldSave = false;
        for (ActionBarType type : ActionBarType.values()) {
            if (!user.isActionBarEnabled(type)) {
                shouldSave = true;
            }
        }
        if (!shouldSave) {
            return;
        }
        MongoCollection<Document> collection = getCollection(MongoDBCollectionName.KEY_VALUES.getCollectionName());
        ActionBarType type = ActionBarType.IDLE;
        String value = String.valueOf(user.isActionBarEnabled(type));
        Document update = collection.findOneAndUpdate(Filters.and(Filters.eq("uid", userId), Filters.eq("data_id", ACTION_BAR_ID), Filters.eq("category_id", Types.NULL), Filters.eq("key_name", type.toString().toLowerCase(Locale.ROOT))), new Document("$set", new Document("value", value)));
        if (update == null) {
            collection.insertOne(new Document("uid", userId).append("data_id", ACTION_BAR_ID).append("category_id", Types.NULL).append("key_name", type.toString().toLowerCase(Locale.ROOT)).append("value", value));
        }
    }

    @Override
    public List<UserState> loadStates(boolean ignoreOnline) {
        List<UserState> states = new ArrayList<>();

        Map<Integer, Map<Skill, Integer>> loadedSkillLevels = new HashMap<>();
        Map<Integer, Map<Skill, Double>> loadedSkillXp = new HashMap<>();

        MongoCollection<Document> collection = getCollection(MongoDBCollectionName.SKILL_LEVELS.getCollectionName());
        List<Document> skill_levels = collection.find().into(new ArrayList<>());
        for (Document document : skill_levels) {
            int userId = document.getInteger("uid");
            String skillName = document.getString("skill_name");
            Skill skill = plugin.getSkillRegistry().get(NamespacedId.fromString(skillName));

            int level = document.getInteger("skill_level");
            double xp = document.getDouble("skill_xp");

            loadedSkillLevels.computeIfAbsent(userId, k -> new HashMap<>()).put(skill, level);
            loadedSkillXp.computeIfAbsent(userId, k -> new HashMap<>()).put(skill, xp);
        }

        MongoCollection<Document> account = getCollection(MongoDBCollectionName.SKILL_ACCOUNT.getCollectionName());
        List<Document> accounts = account.find().into(new ArrayList<>());
        for (Document document : accounts) {
            int userId = document.getInteger("uid");
            UUID uuid = UUID.fromString(document.getString("player_uuid"));

            if (ignoreOnline && userManager.hasUser(uuid)) {
                continue; // Skip if player is online
            }

            double mana = document.getDouble("mana");

            Map<String, StatModifier> statModifiers = loadStatModifiers(uuid, userId);
            Map<String, TraitModifier> traitModifiers = loadTraitModifiers(uuid, userId);

            Map<Skill, Integer> skillLevelMap = loadedSkillLevels.get(userId);
            Map<Skill, Double> skillXpMap = loadedSkillXp.get(userId);

            UserState state = new UserState(uuid, skillLevelMap, skillXpMap, statModifiers, traitModifiers, mana);
            states.add(state);
        }
        return states;
    }
}