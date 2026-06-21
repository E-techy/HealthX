package com.example.healthx

import com.mongodb.client.MongoClients
import org.bson.Document
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.util.Properties

class MongDBDataTest {

    /**
     * Safely reads the database URI from the project's local.properties file.
     */
    private fun getMongoUri(): String {
        val properties = Properties()
        // Unit tests run inside the 'app' module folder, so we step up one directory ("../") to find local.properties
        val localPropertiesFile = File("../local.properties")

        if (!localPropertiesFile.exists()) {
            throw IllegalStateException("local.properties file not found at the project root!")
        }

        properties.load(FileInputStream(localPropertiesFile))
        return properties.getProperty("MONGODB_URI")
            ?: throw IllegalStateException("MONGODB_URI variable is missing from local.properties!")
    }

    @Test
    fun testMongoConnectionAndInsert() {
        // 1. Get the secure connection string
        val uri = getMongoUri()

        println("Connecting to MongoDB Atlas...")

        // 2. Establish connection to the cluster
        MongoClients.create(uri).use { mongoClient ->

            // 3. Select the Database (MongoDB creates it automatically if it doesn't exist)
            val database = mongoClient.getDatabase("HealthX_DB")

            // 4. Select the Collection (This is MongoDB's version of a table/folder)
            val collection = database.getCollection("user_profiles")

            // 5. Create a sample BSON Document matching our UserProfile schema concepts
            val sampleUser = Document("_id", "test_user_001")
                .append("name", "Ashutosh Kumar Singh")
                .append("email", "test@healthx.com")
                .append("gender", "Male")
                .append("accountStatus", "ACTIVE")
                .append("createdAt", java.util.Date())

            // 6. Insert into the database
            println("Inserting test profile for Ashutosh...")
            collection.insertOne(sampleUser)

            // 7. Verify it was actually saved by fetching it back
            val insertedDoc = collection.find(Document("_id", "test_user_001")).first()

            println("\n--- Successfully Retrieved from Cloud ---")
            println(insertedDoc?.toJson())
            println("-----------------------------------------\n")

            // Assert that the document is not null to pass the test
            assert(insertedDoc != null) { "Failed to insert and retrieve the document from Atlas." }

            // 8. Cleanup (Optional but recommended so we don't leave junk in the production DB)
            collection.deleteOne(Document("_id", "test_user_001"))
            println("Test cleanup complete. Sample user removed.")
        }
    }
}