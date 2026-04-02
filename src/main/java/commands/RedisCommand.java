package commands;

import java.io.IOException;
import java.io.OutputStream;

public interface RedisCommand {
    void execute(String[] parts, OutputStream out) throws IOException;
}
