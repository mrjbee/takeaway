package team.monroe.org.takeaway.view;

public interface DynamicListAdapter {
    long getItemId(int position);
    void swapData(int indexOne, int indexTwo);
}
