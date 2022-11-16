package org.openlca.io.refdata;

import java.sql.PreparedStatement;

import org.apache.commons.csv.CSVRecord;
import org.openlca.core.model.ModelType;
import org.openlca.util.Strings;

class ImpactMethodImport extends AbstractImport {

	@Override
	protected String getStatement() {
		return "insert into tbl_impact_methods (id, ref_id, name, description, "
			+ "f_category) values (?, ?, ?, ?, ?)";
	}

	@Override
	protected boolean isValid(CSVRecord row) {
		var refId = Csv.get(row, 0);
		return Strings.notEmpty(refId);
	}

	@Override
	protected void setValues(PreparedStatement stmt, CSVRecord row)
		throws Exception {
		String refId = Csv.get(row, 0);
		long id = seq.get(ModelType.IMPACT_METHOD, refId);
		stmt.setLong(1, id);
		stmt.setString(2, refId);
		stmt.setString(3, Csv.get(row, 1));
		stmt.setString(4, Csv.get(row, 2));
		setRef(stmt, 5, ModelType.CATEGORY, Csv.get(row, 3));
	}
}
