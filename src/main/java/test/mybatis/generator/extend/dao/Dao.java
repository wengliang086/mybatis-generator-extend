package test.mybatis.generator.extend.dao;


public interface Dao<T, P> {

    void save(T t);

    T get(P p);

    void update(T t);

    void delete(P p);

}
