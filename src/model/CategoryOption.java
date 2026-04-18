package model;

/**
 * Lightweight option for the Course Category dropdown in Add / Edit dialogs.
 */
public class CategoryOption {

    private final int    categoryId;
    private final String name;

    public CategoryOption(int categoryId, String name) {
        this.categoryId = categoryId;
        this.name       = name;
    }

    public int    getCategoryId() { return categoryId; }
    public String getName()       { return name; }

    @Override public String toString() { return name; }
}
