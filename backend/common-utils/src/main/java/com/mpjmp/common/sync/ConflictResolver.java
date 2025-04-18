package com.mpjmp.common.sync;

import java.nio.file.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

public class ConflictResolver {
    public enum Strategy { OVERWRITE, RENAME, SKIP }
    
    private final Strategy defaultStrategy;
    
    @Autowired
    public ConflictResolver(Strategy defaultStrategy) {
        this.defaultStrategy = defaultStrategy;
    }

    public Path resolve(Path path, Strategy strategy) {
        while (Files.exists(path)) {
            switch(strategy != null ? strategy : defaultStrategy) {
                case OVERWRITE: return path;
                case SKIP: return null;
                case RENAME: path = addSuffix(path);
            }
        }
        return path;
    }
    
    private Path addSuffix(Path path) {
        String filename = path.getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        
        if (dotIndex > 0) {
            String prefix = filename.substring(0, dotIndex);
            String suffix = filename.substring(dotIndex);
            return path.resolveSibling(prefix + "_copy" + suffix);
        }
        return path.resolveSibling(filename + "_copy");
    }
}
