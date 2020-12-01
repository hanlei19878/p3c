package com.alibaba.p3c.pmd.lang.xml.rule.mybatis;


public class MyBatisDbType {
    private static String dbType = System.getProperty("pmd.myBatisDbType", "oracle");
    public static String changeDbType(String newDbType) {
        dbType = newDbType;
        return dbType;
    }

    public static String getDbType(){
        return dbType;
    }
}
