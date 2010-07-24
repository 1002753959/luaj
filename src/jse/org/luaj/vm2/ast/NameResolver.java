package org.luaj.vm2.ast;

import java.util.List;

import org.luaj.vm2.ast.Exp.NameExp;
import org.luaj.vm2.ast.Exp.VarExp;
import org.luaj.vm2.ast.Exp.VarargsExp;
import org.luaj.vm2.ast.NameScope.NamedVariable;
import org.luaj.vm2.ast.Stat.Assign;
import org.luaj.vm2.ast.Stat.FuncDef;
import org.luaj.vm2.ast.Stat.GenericFor;
import org.luaj.vm2.ast.Stat.LocalAssign;
import org.luaj.vm2.ast.Stat.LocalFuncDef;
import org.luaj.vm2.ast.Stat.NumericFor;

/** 
 * Visitor that resolves names to scopes.
 * Each Name is resolved to a NamedVarible, possibly in a NameScope 
 * if it is a local, or in no named scope if it is a global. 
 */
public class NameResolver extends Visitor {

	private NameScope scope = null;

	private void pushScope() {
		scope = new NameScope(scope);
	}
	private void popScope() {
		scope = scope.outerScope;
	}
	
	public void visit(NameScope scope) {
	}	

	public void visit(Block block) {
		pushScope();
		block.scope = scope;
		super.visit(block);
		popScope();
	}
	
	public void visit(FuncBody body) {
		pushScope();
		scope.functionNestingCount++;
		body.scope = scope;
		super.visit(body);
		popScope();
	}
	
	public void visit(LocalFuncDef stat) {
		defineLocalVar(stat.name);
		super.visit(stat);
	}

	public void visit(NumericFor stat) {
		pushScope();
		stat.scope = scope;
		defineLocalVar(stat.name);
		super.visit(stat);
		popScope();
	}

	public void visit(GenericFor stat) {
		pushScope();
		stat.scope = scope;
		defineLocalVars( stat.names );
		super.visit(stat);
		popScope();
	}

	public void visit(NameExp exp) {
		exp.name.variable = resolveNameReference(exp.name);
		super.visit(exp);
	}
	
	public void visit(FuncDef stat) {
		stat.name.name.variable = resolveNameReference(stat.name.name);
		super.visit(stat);
	}
	
	public void visit(Assign stat) {
		super.visit(stat);
		for ( VarExp v : stat.vars )
			v.markHasAssignment();
	}

	public void visit(LocalAssign stat) {
		visitExps(stat.values);
		defineLocalVars( stat.names );
	}

	public void visit(ParList pars) {
		if ( pars.names != null )
			defineLocalVars(pars.names);
		if ( pars.isvararg )
			scope.define("arg");
		super.visit(pars);
	}
	
	protected void defineLocalVars(List<Name> names) {
		for ( Name n : names ) 
			defineLocalVar(n);
	}

	protected void defineLocalVar(Name name) {
		name.variable = scope.define(name.name);
	}
	
	protected NamedVariable resolveNameReference(Name name) {
		NamedVariable v = scope.find(name.name);
		if ( v.isLocal() && scope.functionNestingCount != v.definingScope.functionNestingCount )
			v.isupvalue = true;
		return v;
	}
}
