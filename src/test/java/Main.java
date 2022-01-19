import java.io.IOException;
import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.*;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        WatchService service = FileSystems.getDefault().newWatchService();

        Path dir = Paths.get("/Users/livio/Desktop/");
        dir.register(service, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);

        while (true) {
            WatchKey key = service.take();
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                Path filePath = dir.resolve(((WatchEvent<Path>) event).context());
                if (ENTRY_CREATE.equals(kind)) {
                    System.out.println("Entry was created on log dir.");
                } else if (ENTRY_MODIFY.equals(kind)) {
                    System.out.println("Entry was modified on log dir.");
                } else if (ENTRY_DELETE.equals(kind)) {
                    System.out.println("Entry was deleted from log dir.");
                }
                System.out.println(filePath.toAbsolutePath());
            }
            key.reset();
        }
    }
}
