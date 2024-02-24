lexer grammar Lang24Lexer;

@header {
	package lang24.phase.lexan;
	import lang24.common.report.*;
	import lang24.data.token.*;
}

@members {
    @Override
	public LocLogToken nextToken() {
		return (LocLogToken) super.nextToken();
	}
}
