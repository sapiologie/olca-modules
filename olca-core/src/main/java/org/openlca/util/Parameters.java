package org.openlca.util;

import java.util.Date;

import gnu.trove.set.hash.TLongHashSet;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.NativeSql;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterScope;
import org.openlca.core.model.Version;
import org.openlca.expressions.FormulaInterpreter;
import org.openlca.expressions.InterpreterException;
import org.openlca.formula.Formulas;

public class Parameters {

	private Parameters() {
	}

	/**
	 * Returns true if the given name is a valid identifier for a parameter. We
	 * allow the same rules as for Java identifiers.
	 */
	public static boolean isValidName(String name) {
		if (name == null)
			return false;
		String id = name.trim();
		if (id.isEmpty())
			return false;
		for (int i = 0; i < id.length(); i++) {
			char c = id.charAt(i);
			if (i == 0 && !Character.isLetter(c))
				return false;
			if (i > 0 && !Character.isJavaIdentifierPart(c))
				return false;
		}

		// TODO: better if we would use the lexer rules here
		FormulaInterpreter interpreter = new FormulaInterpreter();
		interpreter.bind(name, "1");
		try {
			interpreter.eval(name);
		} catch (InterpreterException e) {
			return false;
		}
		return true;
	}

	/**
	 * Renames the given global parameter in the database. Renaming the parameter
	 * means that it is also renamed in all places where it is used: formulas
	 * of exchanges, impact factors, other parameters, and parameter redefinitions.
	 * Formulas of which are in the scope of a local parameter with the same name
	 * are not changed.
	 */
	public static Parameter rename(IDatabase db, Parameter param, String name) {
		if (db == null || param == null)
			throw new NullPointerException("database or parameter is NULL");
		if (param.scope != ParameterScope.GLOBAL) {
			throw new IllegalArgumentException(
					param + " is not defined in the global scope");
		}
		if (!isValidName(name)) {
			throw new IllegalArgumentException(
					name + " is not a valid parameter name");
		}

		// if the parameter has no name or if it is equivalent to the
		// new name, we do not have to change the formulas or redefinitions
		if (Strings.nullOrEmpty(param.name) || eq(param.name, name)) {
			param.name = name;
			Version.incUpdate(param);
			param.lastChange = new Date().getTime();
			return new ParameterDao(db).update(param);
		}

		// collect the IDs of processes and impact categories where
		// a local parameter with the same name is defined
		var localOwners = new TLongHashSet();
		String sql = "select name, f_owner from tbl_parameters";
		NativeSql.on(db).query(sql, r -> {
			long owner = r.getLong(2);
			if (r.wasNull() || owner == 0)
				return true;
			String n = r.getString(1);
			if (eq(n, name) || eq(n, param.name)) {
				localOwners.add(owner);
			}
			return true;
		});

		// rename unbound variables in parameter formulas
		sql = "select f_owner, formula from tbl_parameters" +
				" where formula is not null";
		NativeSql.on(db).updateRows(sql, r -> {
			long owner = r.getLong(1);
			if (owner != 0 && localOwners.contains(owner))
				return true;
			String formula = r.getString(2);
			if (!hasVariable(formula, param.name))
				return true;
			formula = Formulas.renameVariable(
					formula, param.name, name);
			r.updateString(2, formula);
			r.updateRow();
			return true;
		});

		db.clearCache();

		// finally, update the parameter
		param.name = name;
		Version.incUpdate(param);
		param.lastChange = new Date().getTime();
		return new ParameterDao(db).update(param);
	}

	/**
	 * Returns true if both parameter names are equivalent regarding the formula
	 * interpreter.
	 */
	private static boolean eq(String name1, String name2) {
		if (name1 == null || name2 == null)
			return false;
		return Strings.nullOrEqual(
				name1.trim().toLowerCase(),
				name2.trim().toLowerCase());
	}

	private static boolean hasVariable(String formula, String variable) {
		if (formula == null || variable == null)
			return false;
		return Formulas.getVariables(formula)
				.stream()
				.anyMatch(v -> eq(v, variable));
	}
}
