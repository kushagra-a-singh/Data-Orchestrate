// Index for replication_status collection
// Run this in MongoDB shell or via a migration script

db.getCollection('replication_status').createIndex({deviceId: 1, fileId: 1}, {unique: true});
db.getCollection('replication_status').createIndex({fileId: 1});
db.getCollection('replication_status').createIndex({deviceId: 1});
