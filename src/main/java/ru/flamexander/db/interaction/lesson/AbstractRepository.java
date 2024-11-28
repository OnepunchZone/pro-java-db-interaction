package ru.flamexander.db.interaction.lesson;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AbstractRepository<T> {
    private DataSource dataSource;
    private PreparedStatement psInsert;
    private List<Field> cachedFields;
    private Class<T> cls;
    private Field idField;
    private String tableName;

    public AbstractRepository(DataSource dataSource, Class<T> cls) {
        this.dataSource = dataSource;
        this.cls = cls;

        this.checkTableCacheFields(cls);

        this.prepareInsert();
    }

    // Это было ооооочень сложно для меня :(
    // самое страшное, что через пару дней 80% проделанной работы я забуду :(
    private void checkTableCacheFields(Class<T> cls) {

        if (!cls.isAnnotationPresent(RepositoryTable.class)) {
            throw new ORMException(
                    "Класс не предназначен для создания репозитория, отсутствует аннотация @RepositoryTable.");
        }

        this.tableName = cls.getAnnotation(RepositoryTable.class).title();

        this.cachedFields = Arrays.stream(cls.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(RepositoryField.class))
                .collect(Collectors.toList());

        for (Field f : cachedFields) {
            f.setAccessible(true);
        }

        this.idField = Arrays.stream(cls.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(RepositoryIdField.class))
                .findFirst()
                .orElseThrow(() -> new ORMException("Отсутствует поле с аннотацией @RepositoryIdField."));

        idField.setAccessible(true);
    }

    public void save(T entity) {
        try {
            for (int i = 0; i < cachedFields.size(); i++) {
                psInsert.setObject(i + 1, cachedFields.get(i).get(entity));
            }
            psInsert.executeUpdate();
        } catch (Exception e) {
            throw new ORMException("Что-то пошло не так при сохранении: " + entity);
        }
    }

    private void prepareInsert() {
        if (!cls.isAnnotationPresent(RepositoryTable.class)) {
            throw new ORMException(
                    "Класс не предназначен для создания репозитория, не хватает аннотации @RepositoryTable");
        }

        StringBuilder query = new StringBuilder("insert into ");
        query.append(tableName).append(" (");
        // 'insert into users ('
        query.append(cachedFields.stream()
                .map(f -> f.getAnnotation(RepositoryField.class).columName())
                .collect(Collectors.joining(", ")));
        // 'insert into users (login, password, nickname, '
        query.append(") values (");
        // 'insert into users (login, password, nickname) values ('
        for (Field f : cachedFields) {
            query.append("?, ");
        }
        query.setLength(query.length() - 2);
        query.append(");");
        // 'insert into users (login, password, nickname) values (?, ?, ?);'
        try {
            psInsert = dataSource.getConnection().prepareStatement(query.toString());
        } catch (SQLException e) {
            throw new ORMException("Не удалось проинициализировать репозиторий для класса " + cls.getName());
        }
    }

    public T findById(Object id) {
        String query = "select * from " + tableName + " where "
                + idField.getAnnotation(RepositoryField.class).columName() + " = ?";

        try (PreparedStatement ps = dataSource.getConnection().prepareStatement(query)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEntity(rs);
                }
            }
        } catch (Exception e) {
            throw new ORMException("Ошибка поиска в методе findById() с id: " + id);
        }

        return null;
    }

    public List<T> findAll() {
        String query = "select * from " + tableName;
        List<T> results = new ArrayList<>();

        try (PreparedStatement ps = dataSource.getConnection().prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                results.add(mapResultSetToEntity(rs));
            }
        } catch (Exception e) {
            throw new ORMException("Ошибка поиска в методе findAll()");
        }

        return results;
    }

    public void update(T entity) {
        StringBuilder query = new StringBuilder("update ");

        query.append(tableName).append(" set ");
        query.append(cachedFields.stream()
                .map(f -> f.getAnnotation(RepositoryField.class).columName() + " = ?")
                .collect(Collectors.joining(", ")));
        query.append(" WHERE ").append(idField.getAnnotation(RepositoryField.class).columName()).append(" = ?");

        try (PreparedStatement ps = dataSource.getConnection().prepareStatement(query.toString())) {
            int index = 1;
            for (Field field : cachedFields) {
                ps.setObject(index++, field.get(entity));
            }
            ps.setObject(index, idField.get(entity));
            ps.executeUpdate();
        } catch (Exception e) {
            throw new ORMException("Ошибка в методе update(). " + entity);
        }
    }

    public void deleteById(Object id) {
        String query = "delete from " + tableName + " where "
                + idField.getAnnotation(RepositoryField.class).columName() + " = ?";

        try (PreparedStatement ps = dataSource.getConnection().prepareStatement(query)) {
            ps.setObject(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ORMException("Ошибка в методе deleteById() в записи с ID: " + id);
        }
    }

    private T mapResultSetToEntity(ResultSet rs) throws Exception {
        T entity = cls.getDeclaredConstructor().newInstance();

        for (Field field : cachedFields) {
            String columnName = field.getAnnotation(RepositoryField.class).columName();
            Object value = rs.getObject(columnName);
            field.set(entity, value);
        }

        idField.set(entity, rs.getObject(idField.getAnnotation(RepositoryField.class).columName()));

        return entity;
    }
}
