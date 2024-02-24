package lang24;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.stream.Stream;

import lang24.common.report.*;
import lang24.data.token.LocLogToken;
import lang24.phase.lexan.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;

/**
 * The LANG'24 compiler.
 *
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class Compiler {

    /**
     * (Unused but included to keep javadoc happy.)
     */
    private Compiler() {
        throw new Report.InternalError();
    }

    /**
     * All valid phases name of the compiler.
     */
    private static final Vector<String> phaseNames = new Vector<>(
            Arrays.asList("none", "all", "lexan", "synan", "abstr", "seman"));

    /**
     * Names of command line options.
     */
    private static final HashSet<String> cmdLineOptNames = new HashSet<String>(
            Arrays.asList("--src-file-name", "--dst-file-name", "--target-phase", "--logged-phase", "--xml", "--xsl"));

    /**
     * Values of command line options indexed by their command line option name.
     */
    private static final HashMap<String, String> cmdLineOptValues = new HashMap<String, String>();

    /**
     * Returns the value of a command line option.
     *
     * @param cmdLineOptName Command line option name.
     * @return Command line option value.
     */
    public static String cmdLineOptValue(final String cmdLineOptName) {
        return cmdLineOptValues.get(cmdLineOptName);
    }

    /**
     * The compiler's main driver running all phases one after another.
     *
     * @param opts Command line arguments (see {@link lang24}).
     */
    public static void main(final String[] opts) {
        try {
            Report.info("This is LANG'24 compiler:");

            // Scan the command line.
            for (String opt : opts) {
                if (opt.startsWith("--")) {
                    // Command line option.
                    final String cmdLineOptName = opt.replaceFirst("=.*", "");
                    final String cmdLineOptValue = opt.replaceFirst("^[^=]*=", "");
                    if (!cmdLineOptNames.contains(cmdLineOptName)) {
                        Report.warning("Unknown command line option '" + cmdLineOptName + "'.");
                        continue;
                    }
                    if (cmdLineOptValues.get(cmdLineOptName) == null) {
                        // Not yet successfully specified command line option.

                        // Check the value of the command line option.
                        if ((cmdLineOptName.equals("--target-phase") && (!phaseNames.contains(cmdLineOptValue)))
                                || (cmdLineOptName.equals("--logged-phase")
                                && (!phaseNames.contains(cmdLineOptValue)))) {
                            Report.warning("Illegal phase specification in '" + opt + "' ignored.");
                            continue;
                        }

                        cmdLineOptValues.put(cmdLineOptName, cmdLineOptValue);
                    } else {
                        // Repeated specification of a command line option.
                        Report.warning("Command line option '" + opt + "' ignored.");
                    }
                } else {
                    // Source file name.
                    if (cmdLineOptValues.get("--src-file-name") == null) {
                        cmdLineOptValues.put("--src-file-name", opt);
                    } else {
                        Report.warning("Source file '" + opt + "' ignored.");
                    }
                }
            }
            // Check the command line option values.
            if (cmdLineOptValues.get("--src-file-name") == null) {
                try {
                    // Source file has not been specified, so consider using the last modified
                    // lang24 file in the working directory.
                    final String currWorkDir = new File(".").getCanonicalPath();
                    FileTime latestTime = FileTime.fromMillis(0);
                    Path latestPath = null;
                    try (final Stream<Path> paths = Files.walk(Paths.get(currWorkDir))) {
                        for (final Path path : paths.filter(path -> path.toString().endsWith(".lang24")).toArray(Path[]::new)) {
                            final FileTime time = Files.getLastModifiedTime(path);
                            if (time.compareTo(latestTime) > 0) {
                                latestTime = time;
                                latestPath = path;
                            }
                        }
                    }

                    if (latestPath != null) {
                        cmdLineOptValues.put("--src-file-name", latestPath.toString());
                        Report.warning("Source file not specified, using '" + latestPath.toString() + "'.");
                    }
                } catch (final IOException __) {
                    throw new Report.Error("Source file not specified.");
                }

                if (cmdLineOptValues.get("--src-file-name") == null) {
                    throw new Report.Error("Source file not specified.");
                }
            }
            if (cmdLineOptValues.get("--dst-file-name") == null) {
                cmdLineOptValues.put("--dst-file-name",
                        // TODO: Insert the appropriate file suffix.
                        cmdLineOptValues.get("--src-file-name").replaceFirst("\\.[^./]*$", "") + "");
            }
            cmdLineOptValues.putIfAbsent("--target-phase", "all");
            cmdLineOptValues.putIfAbsent("--logged-phase", "none");

            // Carry out the compilation phase by phase.
            while (true) {
                if (cmdLineOptValues.get("--target-phase").equals("none"))
                    break;

                // Lexical analysis.
                if (cmdLineOptValues.get("--target-phase").equals("lexan")) {
                    try (final LexAn lexan = new LexAn()) {
                        final boolean[] error = {false};
                        // Add an error listener to the lexer.
                        lexan.lexer.addErrorListener(new BaseErrorListener() {
                            @Override
                            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                                error[0] = true;
                                throw new Report.Error(new Location(line, charPositionInLine), "Syntax error: " + msg);
                            }
                        });

                        while (lexan.lexer.nextToken().getType() != LocLogToken.EOF) {
                            if (error[0]) {
                                break;
                            }
                        }
                    }
                    break;
                }

                break;
            }

            // Let's hope we ever come this far.
            // But beware:
            // 1. The generated translation of the source file might be erroneous :-o
            // 2. The source file might not be what the programmer intended it to be ;-)
            Report.info("Done.");
        } catch (final Report.Error error) {
            System.err.println(error.getMessage());
            System.exit(1);
        }
    }

}