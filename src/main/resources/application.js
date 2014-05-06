var EXT_ID = 'DBIMPORT_EXTERNAL_MODIFIER';

oracle = {
    nextval: function(sequence) {
        return sql.single('select ' + sequence + '.nextval from DUAL');
    }
};

function tableSize() {
    return db[tableName].length;
}

function field() {
    return db[tableName][iterator][fieldName];
}

function extId(value) {
    db[tableName][iterator][EXT_ID] = value;
    return value;
}

function extByName(tbName, fdName) {
    return oQuery(db[tbName], '[' + fdName + '=' + field() + ']')[0][EXT_ID];
}