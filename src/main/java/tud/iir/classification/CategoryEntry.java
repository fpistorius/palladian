package tud.iir.classification;

import java.io.Serializable;

import org.apache.log4j.Logger;

/**
 * Hold information about how relevant a category is.
 * 
 * @author David Urbansky
 * 
 */
public class CategoryEntry implements Serializable {

    private static final long serialVersionUID = 2420029642880591759L;

    private CategoryEntries categoryEntries;
    private Category category;
    private double absoluteRelevance;
    private double relativeRelevance = -1.0;
    public double bayesRelevance = 1.0;

    public CategoryEntry(CategoryEntries categoryEntries, Category category, double absoluteRelevance) {
        super();
        this.categoryEntries = categoryEntries;
        if (category != null) {
            setCategory(category);
        } else {
            Logger.getRootLogger().warn("A category entry was created with NULL as category");
            setCategory(new Category("-UNASSIGNED-"));
        }
        this.absoluteRelevance = absoluteRelevance;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public double getRelevance() {

        if (!this.categoryEntries.isRelevancesUpToDate()) {
            this.categoryEntries.calculateRelativeRelevances();
        }

        // return getCategory().getPrior() * this.relativeRelevance; // yields lower performance (qualitative)
        // double r = 1000000 * this.categoryEntries.getTermWeight(category) * this.relativeRelevance; // yields lower performance (qualitative)
        // double r = category.getPrior() * this.categoryEntries.getTermWeight(category) * this.relativeRelevance;
        // return r;
        return this.relativeRelevance;
    }

    public void multAbsRel(double factor) {
        this.categoryEntries.setRelevancesUpToDate(false);
        this.absoluteRelevance *= factor;
    }

    protected void setRelativeRelevance(double relevance) {
        this.relativeRelevance = relevance;
    }

    public double getAbsoluteRelevance() {
        return absoluteRelevance;
    }

    public void addAbsoluteRelevance(double value) {
        // If a CategoryEntry is entered, the relative relevances are not up to date anymore.
        this.categoryEntries.setRelevancesUpToDate(false);
        this.absoluteRelevance += value;
    }
    
    public CategoryEntries getCategoryEntries() {
        return categoryEntries;
    }
    
    public void setCategoryEntries(CategoryEntries categoryEntries) {
        this.categoryEntries = categoryEntries;
    }

    @Override
    public String toString() {
        return "CategoryEntry [category=" + category + ", abs. relevance=" + absoluteRelevance + ", rel. relevance=" + getRelevance() + "]";
    }
    
//    // TODO only for debugging, remove this later.
//    @Override
//    protected void finalize() throws Throwable {
//        System.out.println("finalizing " + this);
//        super.finalize();
//    }
}