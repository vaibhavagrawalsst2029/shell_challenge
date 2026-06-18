import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        while (true) {

            System.out.print("$ ");
            System.out.flush();

            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            if (input.equals("exit")) {
                break;
            }

            // Parse the input into arguments handling single quotes, double quotes, and backslashes
            List<String> parsedArgs = parseArguments(input);
            if (parsedArgs.isEmpty()) {
                continue;
            }

            String cmd = parsedArgs.get(0);

            if (cmd.equals("pwd")) {
                System.out.println(System.getProperty("user.dir"));
                continue;
            }

            if (cmd.equals("cd")) {
                String path = parsedArgs.size() > 1 ? parsedArgs.get(1) : "~";
                File target;

                if (path.equals("~")) {
                    String home = System.getenv("HOME");
                    target = new File(home);
                } else if (path.startsWith("/")) {
                    target = new File(path);
                } else {
                    File current = new File(System.getProperty("user.dir"));
                    target = new File(current, path);
                }

                try {
                    File resolved = new File(target.getCanonicalPath());

                    if (resolved.isDirectory()) {
                        System.setProperty("user.dir", resolved.getAbsolutePath());
                    } else {
                        System.out.println("cd: " + path + ": No such file or directory");
                    }

                } catch (Exception e) {
                    System.out.println("cd: " + path + ": No such file or directory");
                }

                continue;
            }

            if (cmd.equals("echo")) {
                // Join all parsed arguments after 'echo' with a single space
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < parsedArgs.size(); i++) {
                    sb.append(parsedArgs.get(i));
                    if (i < parsedArgs.size() - 1) {
                        sb.append(" ");
                    }
                }
                System.out.println(sb.toString());
                continue;
            }

            if (cmd.equals("type")) {
                String targetCmd = parsedArgs.size() > 1 ? parsedArgs.get(1) : "";

                if (targetCmd.equals("echo") || targetCmd.equals("exit") || targetCmd.equals("type") || targetCmd.equals("pwd") || targetCmd.equals("cd")) {
                    System.out.println(targetCmd + " is a shell builtin");
                    continue;
                }

                String pathEnv = System.getenv("PATH");
                String found = null;

                if (pathEnv != null) {
                    String[] paths = pathEnv.split(File.pathSeparator);

                    for (String dir : paths) {
                        File file = new File(dir, targetCmd);

                        if (file.exists() && file.canExecute()) {
                            found = file.getAbsolutePath();
                            break;
                        }
                    }
                }

                if (found != null) {
                    System.out.println(targetCmd + " is " + found);
                } else {
                    System.out.println(targetCmd + ": not found");
                }

                continue;
            }

            // External Command handling (e.g., cat)
            String pathEnv = System.getenv("PATH");
            String found = null;

            if (pathEnv != null) {
                String[] paths = pathEnv.split(File.pathSeparator);

                for (String dir : paths) {
                    File file = new File(dir, cmd);

                    if (file.exists() && file.canExecute()) {
                        found = file.getAbsolutePath();
                        break;
                    }
                }
            }

            if (found == null) {
                System.out.println(cmd + ": command not found");
                continue;
            }

            try {
                ProcessBuilder pb = new ProcessBuilder(parsedArgs);
                pb.inheritIO();

                Process process = pb.start();
                process.waitFor();
                System.out.flush();

            } catch (Exception e) {
                System.out.println("Error executing command");
            }
        }

        scanner.close();
    }

    /**
     * Parses the command line input into a list of arguments.
     * Implements conditional backslash escaping inside double quotes.
     */
    private static List<String> parseArguments(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;
        boolean contentAdded = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            // 1. Handle backslash outside of ALL quotes
            if (c == '\\' && !inSingleQuotes && !inDoubleQuotes) {
                if (i + 1 < input.length()) {
                    currentToken.append(input.charAt(i + 1));
                    i++;
                    contentAdded = true;
                } else {
                    currentToken.append(c);
                    contentAdded = true;
                }
            }
            // 2. Handle backslash INSIDE double quotes (Selective Escaping)
            else if (c == '\\' && inDoubleQuotes) {
                if (i + 1 < input.length()) {
                    char next = input.charAt(i + 1);
                    if (next == '"' || next == '\\' || next == '$' || next == '`') {
                        // It's a special character! Drop the backslash, keep the literal next character
                        currentToken.append(next);
                        i++;
                    } else {
                        // Not a special character! Treat the backslash literally
                        currentToken.append(c);
                    }
                    contentAdded = true;
                } else {
                    currentToken.append(c);
                    contentAdded = true;
                }
            }
            // 3. Toggle single quotes (only if not in double quotes)
            else if (c == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
                contentAdded = true;
            }
            // 4. Toggle double quotes (only if not in single quotes)
            else if (c == '"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
                contentAdded = true;
            }
            // 5. Characters inside active single or double quotes are added directly
            else if (inSingleQuotes || inDoubleQuotes) {
                currentToken.append(c);
            }
            // 6. Normal characters outside of quotes
            else {
                if (Character.isWhitespace(c)) {
                    if (currentToken.length() > 0 || contentAdded) {
                        tokens.add(currentToken.toString());
                        currentToken.setLength(0);
                        contentAdded = false;
                    }
                } else {
                    currentToken.append(c);
                }
            }
        }

        if (currentToken.length() > 0 || contentAdded) {
            tokens.add(currentToken.toString());
        }

        return tokens;
    }
}