package istat.android.data.access.sqlite.utils;

import java.util.HashMap;
import java.util.List;

/**
 * Created by istat on 05/09/17.
 */

public class TableScriptBuilder {

    public TableScriptBuilder append(Class<?> CLass, HashMap<Class, TableScriptFactory.FieldAdapter> adapterHashMap) {

        return this;
    }

    public TableScriptBuilder append(Class<?> CLass) {

        return append(CLass, TableScriptFactory.ADAPTER_MAP_DEFINITION);
    }


    public List<String> create() {
        throw new RuntimeException("Not yet supported.");
    }
}
