package org.hotswap.agent.util;

import org.hotswap.agent.logging.AgentLogger;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

/**
 * IO utils (similar to apache commons).
 */
public class IOUtils {
    private static AgentLogger LOGGER = AgentLogger.getLogger(IOUtils.class);

    // some IDEs remove and recreate whole package multiple times while recompiling -
    // we may need to wait for a file to be available on a filesystem
    private static int WAIT_FOR_FILE_MAX_SECONDS = 5;

    /** URL protocol for a file in the file system: "file" */
    public static final String URL_PROTOCOL_FILE = "file";

    /** URL protocol for a JBoss VFS resource: "vfs" */
    public static final String URL_PROTOCOL_VFS = "vfs";

    /**
     * Download URI to byte array.
     *
     * Wait for the file to exists up to 5 seconds - it may be recreated while IDE recompilation,
     * automatic retry will avoid false errors.
     *
     * @param uri uri to process
     * @return byte array
     * @throws IllegalArgumentException for download problems
     */
    public static byte[] toByteArray(URI uri) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        InputStream inputStream = null;
        int tryCount = 0;
        while (inputStream == null) {
            try {
                inputStream = uri.toURL().openStream();
            } catch (FileNotFoundException e) {
                // some IDEs remove and recreate whole package multiple times while recompiling -
                // we may need to waitForResult for the file.
                if (tryCount > WAIT_FOR_FILE_MAX_SECONDS * 10) {
                    LOGGER.trace("File not found, exiting with exception...", e);
                    throw new IllegalArgumentException(e);
                } else {
                    tryCount++;
                    LOGGER.trace("File not found, waiting...", e);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e1) {
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        try {
            byte[] chunk = new byte[4096];
            int bytesRead;
            InputStream stream = uri.toURL().openStream();

            while ((bytesRead = stream.read(chunk)) > 0) {
                outputStream.write(chunk, 0, bytesRead);
            }

        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        return outputStream.toByteArray();
    }

    /**
     * Convert input stream to a string.
     * @param is stream
     * @return string (at least empty string for empty stream)
     */
    public static String streamToString(InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }


    /**
     * Determine whether the given URL points to a resource in the file system,
     * that is, has protocol "file" or "vfs".
     * @param url the URL to check
     * @return whether the URL has been identified as a file system URL
     * @author Juergen Hoeller (org.springframework.util.ResourceUtils)
     */
    public static boolean isFileURL(URL url) {
        String protocol = url.getProtocol();
        return (URL_PROTOCOL_FILE.equals(protocol) || protocol.startsWith(URL_PROTOCOL_VFS));
    }
}
