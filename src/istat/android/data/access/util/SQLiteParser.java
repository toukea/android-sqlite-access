package istat.android.data.access.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.AssetManager;

/*
 * Copyright (C) 2014 Istat Dev.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * 
 * @author Toukea Tatsi (Istat)
 * 
 */
public class SQLiteParser {

	public static List<String> parseSqlFile(String sqlFile,
			AssetManager assetManager) throws IOException {
		List<String> sqlIns = null;
		InputStream is = assetManager.open(sqlFile);
		try {
			sqlIns = parseSqlFile(is);
		} finally {
			is.close();
		}
		return sqlIns;
	}

	public static List<String> parseSqlFile(Context context, int resSqlFile)
			throws IOException {
		List<String> sqlIns = null;
		InputStream is = context.getResources().openRawResource(resSqlFile);
		try {
			sqlIns = parseSqlFile(is);
		} finally {
			is.close();
		}
		return sqlIns;
	}

	public static List<String> parseSqlFile(InputStream is) throws IOException {
		String script = removeComments(is);
		return verifyStatements(splitSqlScript(script));
		//return splitSqlScript(script);
	}

	private static String removeComments(InputStream is) throws IOException {

		StringBuilder sql = new StringBuilder();

		InputStreamReader isReader = new InputStreamReader(is,"UTF-8");
		try {
			BufferedReader buffReader = new BufferedReader(isReader);
			try {
				String line;
				String multiLineComment = null;
				while ((line = buffReader.readLine()) != null) {
					line = line.trim();

					if (multiLineComment == null) {
						if (line.startsWith("/*")) {
							if (!line.endsWith("}")) {
								multiLineComment = "/*";
							}
						} else if (line.startsWith("{")) {
							if (!line.endsWith("}")) {
								multiLineComment = "{";
							}
						} else if (!line.startsWith("--") && !line.equals("")) {
							sql.append(line);
						}
					} else if (multiLineComment.equals("/*")) {
						if (line.endsWith("*/")) {
							multiLineComment = null;
						}
					} else if (multiLineComment.equals("{")) {
						if (line.endsWith("}")) {
							multiLineComment = null;
						}
					}

				}
			} finally {
				buffReader.close();
			}

		} finally {
			isReader.close();
		}

		return sql.toString();
	}

	private static List<String> splitInsertion(String statement) {
		List<String> out = new ArrayList<String>();
		String INSERT = statement.substring(statement.indexOf("VALUES") + 6);
		int index = 0;

		while (true) {
			int lastIndex = index;
			index = INSERT.indexOf(",(", lastIndex);
			try {
				out.add(INSERT.substring(lastIndex, index));
				index++;
			} catch (Exception e) {
				break;
			}
		}

		return out;
	}

	private static List<String> makeInsertBundle(String header,
			List<String> insertions, int size) {
		List<String> out = new ArrayList<String>();
		for (String insert : insertions)
			out.add(header + insert);
		return out;
	}

	private static List<String> verifyStatements(List<String> statements) {
		List<String> out = new ArrayList<String>();
		for (String statement : statements) {// INSERT
			if (statement.length() > 7) {
				String begginStatement = statement.substring(0, 6)
						.toLowerCase();
				if (begginStatement.equals("insert")) {
					String insertheader = statement.substring(0,
							statement.indexOf("VALUES") + 6);

					out.addAll(makeInsertBundle(insertheader,
							splitInsertion(statement), 0));

				} else {
					out.add(statement);
				}

			}
		}

		return out;
	}

	private static List<String> splitSqlScript(String script) {
		List<String> statements = new ArrayList<String>();
		String[] statementsTable = script.split(";");
		for (String tmp : statementsTable)
			statements.add(tmp.trim()+";");
		return statements;
	}

}