# Lang24 Compiler

A project for university course "Compilers"

## About Lang24 language
See [language specification](lang24_specs.md).

## Building

Simply type
```bash
./gradlew build
```
in terminal. The output jar file will be in `build/libs/` directory.

## Running

To run the compiler, use
```bash
./gradlew run -PsrcFileName=test_file
```
where `test_file` is the name of the file in `prg/` directory, excluding file extension.

Available arguments are (all must be used with `-P` prefix):
* `srcFileName` - name of the source file in `prg/` directory
* `targetPhase` - phase of the compiler to run (default is `all`)
* `loggedPhases` - list of phases to log (default is `all`)
* `xsl` - path to xsl file for generating AST xml (default is `lib/xsl/`)

### Compiling with make

1. Download [antlr](https://www.antlr.org/download/antlr-4.13.1-complete.jar) to project `lib/` folder.
2. Execute following command in project root directory:
```bash
make all
```

### Running with make

1. Write a file `filename.lang24` in `prg/` directory.
2. Execute following command in `prg/` directory:
```bash
make TARGETPHASE=lexan filename
```
where `filename` is the name of the the file, excluding file extension.
