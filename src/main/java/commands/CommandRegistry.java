package commands;

import storage.RedisDatabase;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class CommandRegistry {
    private final Map<String,RedisCommand> commands = new HashMap<>();
    public CommandRegistry(RedisDatabase db){
        commands.put("PING", (parts, out) -> out.write("+PONG\r\n".getBytes()));
        commands.put("GET", new GetCommand(db));
        commands.put("SET",new SetCommand(db));
        commands.put("RPUSH",new RPushCommand(db));
        commands.put("LRANGE", new LRangeCommand(db));
        commands.put("ECHO", new EchoCommand());
        commands.put("LPUSH", new LPushCommand(db));
        commands.put("LLEN", new LLenCommand(db));
    }

    public void handle(String[] parts, OutputStream out) throws IOException {
        String commandName = parts[2].toUpperCase();
        RedisCommand cmd = commands.get(commandName);
        if (cmd != null){
            cmd.execute(parts, out);
        } else {
            out.write("-ERR unknown comand\r\n".getBytes());
        }
    }
}
