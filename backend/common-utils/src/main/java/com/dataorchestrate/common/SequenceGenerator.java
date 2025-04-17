package com.dataorchestrate.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.stereotype.Component;

@Component
public class SequenceGenerator {
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    /**
     * Generates a new sequence number for the given sequence name
     * @param sequenceName The name of the sequence
     * @return The next value in the sequence
     */
    public long generateSequence(String sequenceName) {
        DatabaseSequence counter = mongoTemplate.findAndModify(
            Query.query(Criteria.where("_id").is(sequenceName)),
            new Update().inc("seq", 1),
            FindAndModifyOptions.options().returnNew(true).upsert(true),
            DatabaseSequence.class);
        
        return counter != null ? counter.getSeq() : 1;
    }
    
    /**
     * Entity class to represent a database sequence
     */
    public static class DatabaseSequence {
        private String id;
        private long seq;
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public long getSeq() {
            return seq;
        }
        
        public void setSeq(long seq) {
            this.seq = seq;
        }
    }
}
