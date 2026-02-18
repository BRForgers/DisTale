package one.armelin.distale.utils;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class ObservableCopyOnWriteArrayList<E> extends CopyOnWriteArrayList<E> {

    private final Consumer<E> onAdd;

    public ObservableCopyOnWriteArrayList(Consumer<E> onAdd) {
        this.onAdd = onAdd;
    }

    @Override
    public boolean add(E e) {
        boolean result = super.add(e);
        onAdd.accept(e);
        return result;
    }

    @Override
    public void add(int index, E e) {
        super.add(index, e);
        onAdd.accept(e);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean result = super.addAll(c);
        if (result) c.forEach(onAdd);
        return result;
    }
}
