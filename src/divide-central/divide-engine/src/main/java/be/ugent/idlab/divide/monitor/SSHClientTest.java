package be.ugent.idlab.divide.monitor;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.config.keys.loader.KeyPairResourceLoader;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.server.config.keys.AuthorizedKeysAuthenticator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Collection;
import java.util.EnumSet;

public class SSHClientTest {

    public static void main(String[] args) throws IOException, GeneralSecurityException {

        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();

            // using the client for multiple sessions...
            try (ClientSession session = client.connect("mathias", "10.42.0.1", 22)
                    .verify(1000)
                    .getSession()) {

                KeyPairResourceLoader loader = SecurityUtils.getKeyPairResourceParser();
                Collection<KeyPair> keys = loader.loadKeyPairs(null,
                        Paths.get("/home/mathias/.ssh/id_rsa"),
                        FilePasswordProvider.of("")); // requires Ubuntu password
                for (KeyPair kp : keys) {
                    session.addPublicKeyIdentity(kp);
                }

//                FileKeyPairProvider provider = new FileKeyPairProvider(
//                        Paths.get("/home/mathias/.jFed/login-certs/df480106a205a4f0e36cce3cac21e5c5.pem"));
//                provider.setPasswordFinder(FilePasswordProvider.of("mathID21-8"));
//                session.setKeyIdentityProvider(provider);
//                session.setKeyIdentityProvider(provider); // for password-less authentication

                session.auth();

                // start using the session to run commands, do SCP/SFTP,
                // create local/remote port forwarding, etc...
                try (OutputStream stdout = new ByteArrayOutputStream();
                     OutputStream stderr = new ByteArrayOutputStream();
                     ClientChannel channel = session.createExecChannel("hostname")) {
                    channel.setOut(stdout);
                    channel.setErr(stderr);
                    channel.open();
                    // wait (forever) for the channel to close - signalling command finished
                    channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), 0L);

                    System.out.println(stdout);
                    System.out.println(stderr);
                }
            }
        }

    }

}
