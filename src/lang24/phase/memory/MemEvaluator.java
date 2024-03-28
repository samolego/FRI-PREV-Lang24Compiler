package lang24.phase.memory;

import java.util.*;

import lang24.data.ast.tree.*;
import lang24.data.ast.tree.defn.*;
import lang24.data.ast.tree.expr.AstCallExpr;
import lang24.data.ast.tree.type.AstRecType;
import lang24.data.ast.tree.type.AstStrType;
import lang24.data.ast.tree.type.AstUniType;
import lang24.data.ast.visitor.*;
import lang24.data.mem.*;
import lang24.data.type.*;
import lang24.data.type.visitor.*;
import lang24.phase.seman.SemAn;

/**
 * Computing memory layout: stack frames and variable accesses.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class MemEvaluator implements AstFullVisitor<Object, Object> {

}
