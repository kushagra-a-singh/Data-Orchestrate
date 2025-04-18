import org.springframework.data.mongodb.repository.MongoRepository;
import com.mpjmp.orchestrator.model.SyncHistory;
import java.util.List;

public interface SyncHistoryRepository extends MongoRepository<SyncHistory, String> {
    List<SyncHistory> findByDeviceIdOrderByTimestampDesc(String deviceId);
    List<SyncHistory> findByFileIdOrderByTimestampDesc(String fileId);
}
