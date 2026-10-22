package shell.ui;

import shell.config.Config;
import shell.core.ShellContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public class PromptBuilder {
    private static final String RESET = "\033[0m";
    private static final String GREEN = "\033[32m";
    private static final String BLUE = "\033[94m";
    private static final String RED = "\033[31m";
    private static final String WHITE = "\033[37m";
    private static final String BRIGHT_WHITE = "\033[97m";

    public static String getShellPrompt(int lastExitCode, Duration lastExecTime, ShellContext context) {
        /*
        The Idea:
        ┌──(userName Config.emoji osName)-[workingDirectory]-[branchName] *Right alignment*   execTime  x/v
        └─$ * Next prompt starts here *

        example (The emojis might look broken in IntelliJ or others code editors):
        ┌──(avishai🐧tuxedo-os)-[~/development/Java/shell]-[main]                                   41ms  
        └─$
         */
        String execTimeStr = getFormattedExecTime(lastExecTime);
        String rightAlignSeq = getRightAlignSeq(lastExitCode, execTimeStr);

        String wd = getFormattedWd(context.getWd().toString());
        String userHostInfo = Config.userName + Config.emoji + Config.osName;

        String branchName = getBranchName(context);
        String branchSegment = branchName != null
                ? String.format("-[%s\uE0A0%s%s]", WHITE, branchName, GREEN)
                : "";

        return String.format("%s┌──(%s%s%s)-[%s%s%s]%s%s\n%s└─%s$ %s",
                GREEN,
                BLUE, userHostInfo, GREEN,
                BRIGHT_WHITE, wd, GREEN,
                branchSegment,
                rightAlignSeq,
                GREEN,
                BLUE,
                RESET);
    }

    private static String getRightAlignSeq(int lastExitCode, String execTimeStr) {
        String statusEmoji = lastExitCode == 0 ? "\uF42E" : "\uEA76";
        String statusColor = lastExitCode == 0 ? GREEN : RED;

        int visibleRightLen = execTimeStr.length() + 3; // +2 space, +1 emoji

        return "\033[s"                             // Save cursor position.
                + "\033[999C"                       // Move cursor to far right margin.
                + "\033[" + visibleRightLen + "D"   // Move cursor left.
                + WHITE + execTimeStr + RESET
                + "  "                              // Added space between time and emoji
                + statusColor + statusEmoji + RESET
                + "\033[u";                         // Restore cursor position.
    }

    public static String getFormattedExecTime(Duration duration) {
        if (duration.toNanos() < 1_000_000) return "0ms";

        long days = duration.toDaysPart();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        long millis = duration.toMillisPart();

        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else if (seconds > 0) {
            return String.format("%ds %dms", seconds, millis);
        } else {
            return String.format("%dms", millis);
        }
    }

    public static String getFormattedWd(String wd) {
        // Change the home directory path to "~"

        // IMPORTANT //
        // String.replaceFirst() doesn't take a literal string, but a regex instead.
        // It means that using replaceFirst won't work on Windows,
        // because when the working directory contains backslashes (which it is usually in Windows) -
        // the backslash will corrupt the next character, and I'll get java.util.regex.PatternSyntaxException
        if (wd.startsWith(Config.homeDirectoryStr)) {
            wd = "~" + wd.substring(Config.homeDirectoryStr.length());
        }
        return wd;
    }

    public static String getBranchName(ShellContext context) {
        //todo this is an AI generated code that I haven't tested.
        // I'm sure there is a git command for that which will be much more safer
        // but I'm trying to do it in the fastest way possible
        // I need to test the other option to compare performances.
        Path current = context.getWd();

        // Walk up the directory tree to support subdirectories within a git repo
        while (current != null) {
            Path gitPath = current.resolve(".git");

            //todo Check if this could be a bottleneck in a very deep path.
            if (!Files.exists(gitPath)) {
                current = current.getParent();
                continue;
            }

            try {
                // Handle git worktrees or submodules (.git is a file instead of a directory)
                if (Files.isRegularFile(gitPath)) {
                    String gitFileContent = Files.readString(gitPath).trim();
                    if (gitFileContent.startsWith("gitdir: ")) {
                        gitPath = current.resolve(gitFileContent.substring(8).trim());
                    } else {
                        return null;
                    }
                }

                Path headPath = gitPath.resolve("HEAD");
                if (Files.exists(headPath)) {
                    String headContent = Files.readString(headPath).trim();

                    if (headContent.startsWith("ref: refs/heads/")) {
                        return headContent.substring(16);
                    } else {
                        // Detached HEAD state - return the short commit hash
                        return headContent.substring(0, Math.min(headContent.length(), 7));
                    }
                }
            } catch (Exception ignore) {
            }
            return null;
        }

        return null;
    }
}
