package com.memeasaur.potpissersdefault.Util.Serialization.IO;

import org.bukkit.Bukkit;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class Methods {
    public static void handlePotpissersExceptions(CompletableFuture<?> future, Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        plugin.getLogger().severe(sw.toString());
        if (isExceptionShutdownable)
            Bukkit.shutdown();
        else if (future != null)
            future.completeExceptionally(e);
        throw new RuntimeException(e);
    }

    public static CompletableFuture<Void> writeBinaryFile(String fileName, Object object) {
        CompletableFuture<Void> futureVoid = new CompletableFuture<>();

        SCHEDULER.runTaskAsynchronously(plugin, () -> {
            String tempFilename = fileName + ".tmp";
            Path tempFileNamePath = Path.of(tempFilename);
            try {
                ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tempFilename));
                oos.writeObject(object);
                oos.close();
                Files.move(tempFileNamePath, Path.of(fileName), REPLACE_EXISTING, ATOMIC_MOVE);
                SCHEDULER.runTask(plugin, () ->
                        futureVoid.complete(null));
            } catch (IOException e) {
                handlePotpissersExceptions(futureVoid, e);
            }
        });

        return futureVoid;
    }
    public static CompletableFuture<Void> writeBukkitBinaryFile(String fileName, Object object) {
        CompletableFuture<Void> futureVoid = new CompletableFuture<>();

        SCHEDULER.runTaskAsynchronously(plugin, () -> {
            String tempFileName = fileName + ".tmp";
            Path tempFileNamePath = Path.of(tempFileName);
            try {
                BukkitObjectOutputStream boos = new BukkitObjectOutputStream(new FileOutputStream(tempFileName));
                boos.writeObject(object);
                boos.close();
                Files.move(tempFileNamePath, Path.of(fileName), REPLACE_EXISTING, ATOMIC_MOVE);
                SCHEDULER.runTask(plugin, () ->
                        futureVoid.complete(null));
            } catch (IOException e) {
                handlePotpissersExceptions(futureVoid, e);
            }
        });

        return futureVoid;
    }
    public static CompletableFuture<byte[]> serializeBinary(Object object) {
        CompletableFuture<byte[]> futureBytes = new CompletableFuture<>();

        SCHEDULER.runTaskAsynchronously(plugin, () -> {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
                objectOutputStream.writeObject(object);
                SCHEDULER.runTask(plugin, () ->
                        futureBytes.complete(byteArrayOutputStream.toByteArray()));
            } catch (IOException e) {
                handlePotpissersExceptions(futureBytes, e);
            }
        });

        return futureBytes;
    }
    public static CompletableFuture<byte[]> serializeBukkitBinary(Object object) {
        CompletableFuture<byte[]> futureBytes = new CompletableFuture<>();

        SCHEDULER.runTaskAsynchronously(plugin, () -> {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try (BukkitObjectOutputStream bukkitObjectOutputStream = new BukkitObjectOutputStream(byteArrayOutputStream)) {
                bukkitObjectOutputStream.writeObject(object);
                SCHEDULER.runTask(plugin, () ->
                        futureBytes.complete(byteArrayOutputStream.toByteArray()));
            } catch (IOException e) {
                handlePotpissersExceptions(futureBytes, e);
            }
        });

        return futureBytes;
    }
    public static <T> CompletableFuture<Optional<T>> fetchBinaryFileObject(String filename, Class<T> clazz) {
        CompletableFuture<Optional<T>> futureObject = new CompletableFuture<>();

        SCHEDULER.runTaskAsynchronously(plugin, () -> {
            if (!Files.exists(Paths.get(filename)))
                futureObject.complete(Optional.empty());
            else {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
                    Object readObject = ois.readObject();
                    SCHEDULER.runTask(plugin, () ->
                            futureObject.complete(Optional.of(clazz.cast(readObject))));
                } catch (IOException | ClassNotFoundException e) {
                    handlePotpissersExceptions(futureObject, e);
                }
            }
        });

        return futureObject;
    }
    public static <T> CompletableFuture<Optional<T>> fetchBukkitBinaryFileObject(String fileName, Class<T> clazz) {
        CompletableFuture<Optional<T>> futureObject = new CompletableFuture<>();

        SCHEDULER.runTaskAsynchronously(plugin, () -> {
            if (!Files.exists(Paths.get(fileName)))
                futureObject.complete(Optional.empty());
            else {
                try (BukkitObjectInputStream bois = new BukkitObjectInputStream(new FileInputStream(fileName))) {
                    Object readObject = bois.readObject();
                    SCHEDULER.runTask(plugin, () ->
                            futureObject.complete(Optional.of(clazz.cast(readObject))));
                } catch (IOException | ClassNotFoundException e) {
                    handlePotpissersExceptions(futureObject, e);
                }
            }
        });

        return futureObject;
    }
    public static <T> T getBukkitObjectBlocking(byte[] bytes, Class<T> clazz) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        try (BukkitObjectInputStream bukkitObjectInputStream = new BukkitObjectInputStream(byteArrayInputStream)) {
            return clazz.cast(bukkitObjectInputStream.readObject());
        }
    }
    public static <T> CompletableFuture<T> fetchBukkitObject(@Nonnull byte[] bytes, Class<T> clazz) {
        CompletableFuture<T> futureBukkitObject = new CompletableFuture<>();

        SCHEDULER.runTaskAsynchronously(plugin, () -> {
            try {
                T readObject = getBukkitObjectBlocking(bytes, clazz);
                SCHEDULER.runTask(plugin, () ->
                        futureBukkitObject.complete(clazz.cast(readObject)));
            } catch (IOException | ClassNotFoundException e) {
                handlePotpissersExceptions(futureBukkitObject, e);
            }
        });

        return futureBukkitObject;
    }
    public static <T> CompletableFuture<T> fetchBinaryObject(@Nonnull byte[] bytes, Class<T> clazz) {
        CompletableFuture<T> futureBukkitObject = new CompletableFuture<>();

        SCHEDULER.runTaskAsynchronously(plugin, () -> {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            try (ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
                Object readObject = objectInputStream.readObject();
                SCHEDULER.runTask(plugin, () ->
                        futureBukkitObject.complete(clazz.cast(readObject)));
            } catch (IOException | ClassNotFoundException e) {
                handlePotpissersExceptions(futureBukkitObject, e);
            }
        });

        return futureBukkitObject;
    }
    // Claims start
    public static CompletableFuture<Void> handleBlockingFileSerialization(String taskName, CompletableFuture<Void> newTask) {
        CompletableFuture<Void> task = serializationTasks.get(taskName);
        if (!task.isDone())
            task.cancel(true);
        blockingTasks.add(newTask);
        serializationTasks.put(taskName, task);
        return task;
    }
    // Claims end
}
