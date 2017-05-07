package JSON;

/**
 * Created by User on 07.05.2017.
 */
public class Sample {
    private Object[] arr;

    public void setArr(Object[] a) { arr = a; }

    public void setArr(int b, Object a) { arr[b] = a; }

    public Object[] getArr() { return arr; }

    public Object getArr(int b) { return arr[b]; }
}
