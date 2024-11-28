package ru.flamexander.db.interaction.lesson;

import java.io.IOException;
import java.sql.SQLException;

public class MockChatServer {
    public static void main(String[] args) {
        DataSource dataSource = null;
        try {
            System.out.println("Сервер чата запущен");
            dataSource = new DataSource("jdbc:h2:file:./db;MODE=PostgreSQL");
            dataSource.connect();

            DbMigrator dbMigrator = new DbMigrator(dataSource);
            dbMigrator.migrate();

            AbstractRepository<User> usersRepository = new AbstractRepository<>(dataSource, User.class);

            User user1 = new User(null, "B", "B", "B");
            User user2 = new User(null, "C", "C", "C");
            usersRepository.save(user1);
            usersRepository.save(user2);
            System.out.println(usersRepository.findAll());

            System.out.println(usersRepository.findById(2));
            usersRepository.deleteById(1);
            System.out.println(usersRepository.findAll());

            User updateUser2 = new User(2L, "C", "dfdfKKLLL", "hjsdh");
            usersRepository.update(updateUser2);

            System.out.println(usersRepository.findAll());


//            AuthenticationService authenticationService = new AuthenticationService(usersDao);
//            UsersStatisticService usersStatisticService = new UsersStatisticService(usersDao);
//            BonusService bonusService = new BonusService(dataSource);
//            bonusService.init();

//            authenticationService.register("A", "A", "A");
            // Основная работа сервера чата
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        } finally {
            if (dataSource != null) {
                dataSource.close();
            }
            System.out.println("Сервер чата завершил свою работу");
        }
    }
}
