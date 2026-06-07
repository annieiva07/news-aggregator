package com.newsaggregator.service;

import com.newsaggregator.parser.ParserManager;
import com.newsaggregator.storage.DatabaseConnection;
import com.newsaggregator.storage.NewsRepository;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SchedulerServiceTest {

    private static SchedulerService schedulerService;

    @BeforeAll
    static void setUp() {
        System.setProperty("db.url", "jdbc:sqlite::memory:");
        DatabaseConnection.getConnection();
        NewsRepository mockRepository = Mockito.mock(NewsRepository.class);
        ParserManager parserManager = new ParserManager(mockRepository);
        schedulerService = new SchedulerService(parserManager);
    }

    @AfterAll
    static void tearDown() {
        schedulerService.shutdown();
        DatabaseConnection.closeConnection();
    }

    @Test
    @Order(1)
    void initiallyNotRunning() {
        assertFalse(schedulerService.isRunning());
    }

    @Test
    @Order(2)
    void startSetsRunningTrue() {
        schedulerService.start(60);
        assertTrue(schedulerService.isRunning());
        schedulerService.stop();
    }

    @Test
    @Order(3)
    void stopSetsRunningFalse() {
        schedulerService.start(60);
        schedulerService.stop();
        assertFalse(schedulerService.isRunning());
    }

    @Test
    @Order(4)
    void getIntervalReturnsSetValue() {
        schedulerService.start(45);
        assertEquals(45, schedulerService.getIntervalMinutes());
        schedulerService.stop();
    }

    @Test
    @Order(5)
    void restartChangesInterval() {
        schedulerService.start(10);
        schedulerService.start(20);
        assertEquals(20, schedulerService.getIntervalMinutes());
        schedulerService.stop();
    }

    @Test
    @Order(6)
    void stopWhenNotRunningIsIdempotent() {
        assertFalse(schedulerService.isRunning());
        assertDoesNotThrow(() -> schedulerService.stop());
        assertFalse(schedulerService.isRunning());
    }

    @Test
    @Order(7)
    void startStopStartAgainWorks() {
        schedulerService.start(30);
        schedulerService.stop();
        schedulerService.start(15);
        assertTrue(schedulerService.isRunning());
        assertEquals(15, schedulerService.getIntervalMinutes());
        schedulerService.stop();
    }

    @Test
    @Order(8)
    void shutdownStopsScheduler() {
        SchedulerService local = new SchedulerService(new ParserManager(Mockito.mock(NewsRepository.class)));
        local.start(60);
        assertTrue(local.isRunning());
        local.shutdown();
        assertFalse(local.isRunning());
    }

    @Test
    @Order(9)
    void internalUpdateCallsParserManager() throws Exception {
        ParserManager mockPm = Mockito.mock(ParserManager.class);
        doNothing().when(mockPm).parseAndSave();
        SchedulerService local = new SchedulerService(mockPm);

        Method method = SchedulerService.class.getDeclaredMethod("update");
        method.setAccessible(true);
        method.invoke(local);

        verify(mockPm, times(1)).parseAndSave();
    }
}
