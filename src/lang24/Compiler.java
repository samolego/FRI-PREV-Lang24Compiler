package lang24;

import lang24.common.report.Report;
import lang24.phase.abstr.Abstr;
import lang24.phase.abstr.AbstrLogger;
import lang24.phase.asmgen.AsmGen;
import lang24.phase.imcgen.IG2;
import lang24.phase.imcgen.ImcGen;
import lang24.phase.imcgen.ImcLogger;
import lang24.phase.imclin.ChunkGenerator;
import lang24.phase.imclin.ImcLin;
import lang24.phase.imclin.Interpreter;
import lang24.phase.lexan.LexAn;
import lang24.phase.livean.LiveAn;
import lang24.phase.memory.MemEvaluator;
import lang24.phase.memory.MemLogger;
import lang24.phase.memory.Memory;
import lang24.phase.regall.RegAll;
import lang24.phase.seman.LValResolver;
import lang24.phase.seman.NameResolver;
import lang24.phase.seman.SemAn;
import lang24.phase.seman.SemAnLogger;
import lang24.phase.seman.TypeResolver;
import lang24.phase.synan.SynAn;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import static lang24.phase.regall.RegAlloc.MAX_REGISTERS;

/**
 * The LANG'24 compiler.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class Compiler {

	/** (Unused but included to keep javadoc happy.) */
	private Compiler() {
		throw new Report.InternalError();
	}

	/** All valid phases name of the compiler. */
	private static final Vector<String> phaseNames = new Vector<>(Arrays.asList("none", "all", "lexan", "synan",
            "abstr", "seman", "memory", "imcgen", "imclin", "asmgen", "livean", "regall"));

	/** Names of command line options. */
	private static final HashSet<String> cmdLineOptNames = new HashSet<String>(
			Arrays.asList("--src-file-name", "--dst-file-name", "--target-phase", "--logged-phase", "--xml", "--xsl", "--num-regs"));

	/** Values of command line options indexed by their command line option name. */
	private static final HashMap<String, String> cmdLineOptValues = new HashMap<String, String>();

	/**
	 * Returns the value of a command line option.
	 *
	 * @param cmdLineOptName Command line option name.
	 * @return Command line option value.
	 */
	public static final String cmdLineOptValue(final String cmdLineOptName) {
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
                        continue;
                    }
                } else {
                    // Source file name.
                    if (cmdLineOptValues.get("--src-file-name") == null) {
                        cmdLineOptValues.put("--src-file-name", opt);
                    } else {
                        Report.warning("Source file '" + opt + "' ignored.");
                        continue;
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
					for (final Path path : java.nio.file.Files.walk(Paths.get(currWorkDir))
							.filter(path -> path.toString().endsWith(".lang24")).toArray(Path[]::new)) {
						final FileTime time = Files.getLastModifiedTime(path);
						if (time.compareTo(latestTime) > 0) {
							latestTime = time;
							latestPath = path;
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
			if (cmdLineOptValues.get("--target-phase") == null)
				cmdLineOptValues.put("--target-phase", "all");
			if (cmdLineOptValues.get("--logged-phase") == null)
				cmdLineOptValues.put("--logged-phase", "none");

			// Carry out the compilation phase by phase.
			while (true) {

				if (cmdLineOptValues.get("--target-phase").equals("none"))
					break;

				// Lexical analysis.
				if (cmdLineOptValues.get("--target-phase").equals("lexan")) {
					try (final LexAn lexan = new LexAn()) {
						while (lexan.lexer.nextToken().getType() != lang24.data.token.LocLogToken.EOF) {
						}
					}
					break;
				}

				// Syntax analysis.
				try (LexAn lexan = new LexAn(); SynAn synan = new SynAn(lexan)) {
					SynAn.tree = synan.parser.source();
					synan.log(SynAn.tree);
				}
				if (cmdLineOptValues.get("--target-phase").equals("synan"))
					break;

				// Abstract syntax.
				try (Abstr abstr = new Abstr()) {
					Abstr.tree = SynAn.tree.ast;
					SynAn.tree = null;
					AbstrLogger logger = new AbstrLogger(abstr.logger);
					Abstr.tree.accept(logger, "AstDefn");
				}
				if (cmdLineOptValues.get("--target-phase").equals("abstr"))
					break;

				// Semantic analysis.
				try (SemAn seman = new SemAn()) {
					Abstr.tree.accept(new NameResolver(), null);
					Abstr.tree.accept(new LValResolver(), null);
					Abstr.tree.accept(new TypeResolver(), null);
					AbstrLogger logger = new AbstrLogger(seman.logger);
					logger.addSubvisitor(new SemAnLogger(seman.logger));
					Abstr.tree.accept(logger, "AstDefn");
				}
				if (cmdLineOptValues.get("--target-phase").equals("seman"))
					break;

				// Memory layout.
				try (Memory memory = new Memory()) {
					Abstr.tree.accept(new MemEvaluator(), null);
					AbstrLogger logger = new AbstrLogger(memory.logger);
					logger.addSubvisitor(new SemAnLogger(memory.logger));
					logger.addSubvisitor(new MemLogger(memory.logger));
					Abstr.tree.accept(logger, "AstDefn");
				}
				if (cmdLineOptValues.get("--target-phase").equals("memory"))
					break;

				// Intermediate code generation.
				try (ImcGen imcGen = new ImcGen()) {
					Abstr.tree.accept(new IG2(), null);
					AbstrLogger logger = new AbstrLogger(imcGen.logger);
					logger.addSubvisitor(new SemAnLogger(imcGen.logger));
					logger.addSubvisitor(new MemLogger(imcGen.logger));
					logger.addSubvisitor(new ImcLogger(imcGen.logger));
					Abstr.tree.accept(logger, "AstDefn");
				}
				if (cmdLineOptValues.get("--target-phase").equals("imcgen"))
					break;

				// Linearization of intermediate code.
				try (ImcLin imclin = new ImcLin()) {
					Abstr.tree.accept(new ChunkGenerator(), null);
					imclin.log();

					if (Boolean.parseBoolean(System.getenv().getOrDefault("INTP_MODE", "false"))) {
						Interpreter interpreter = new Interpreter(ImcLin.dataChunks(), ImcLin.codeChunks());
						System.out.println("EXIT CODE: " + interpreter.run("_main"));
					}
				}
				if (cmdLineOptValues.get("--target-phase").equals("imclin"))
					break;

				// Machine code generation.
				try (AsmGen asmgen = new AsmGen()) {
					asmgen.genAsmCodes();
					asmgen.log();
				}
				if (cmdLineOptValues.get("--target-phase").equals("amsgen"))
					break;


				// Liveliness analysis
				try (var livean = new LiveAn()) {
					livean.analysis();
					livean.log();
				}
				if (cmdLineOptValues.get("--target-phase").equals("livean"))
					break;



				// Register allocation
				MAX_REGISTERS = Integer.parseInt(cmdLineOptValues.get("--num-regs"));
				try (var regalloc = new RegAll()) {
					regalloc.allocate();
					regalloc.log();
				}
				if (cmdLineOptValues.get("--target-phase").equals("regall"))
					break;

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