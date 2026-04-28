package model;

/**
 * Lightweight model for one sub-department entry.
 * Used to populate the department combo boxes in the Add/Edit User form.
 */
public class SubDeptItem {

    private final int    subDeptId;
    private final int    majorDeptId;
    private final String name;
    private final String code;
    private final String majorDeptName;

    public SubDeptItem(int subDeptId, int majorDeptId,
                       String name, String code, String majorDeptName) {
        this.subDeptId    = subDeptId;
        this.majorDeptId  = majorDeptId;
        this.name         = name;
        this.code         = code;
        this.majorDeptName = majorDeptName;
    }

    public int    getSubDeptId()    { return subDeptId; }
    public int    getMajorDeptId()  { return majorDeptId; }
    public String getName()         { return name; }
    public String getCode()         { return code; }
    public String getMajorDeptName(){ return majorDeptName; }

    /** Label shown in combo box: e.g. "Computer Science (CS) — Computing" */
    @Override
    public String toString() {
        return name + " (" + code + ")  —  " + majorDeptName;
    }
}