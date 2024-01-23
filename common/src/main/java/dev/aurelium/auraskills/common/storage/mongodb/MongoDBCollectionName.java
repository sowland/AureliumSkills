package dev.aurelium.auraskills.common.storage.mongodb;

public enum MongoDBCollectionName {
    // todo api get uid
    SKILL_ACCOUNT("skill_account"), SKILL_LEVELS("skill_levels"), KEY_VALUES("key_values");

    private final String collectionName;

    MongoDBCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public String getCollectionName() {
        return collectionName;
    }
}
