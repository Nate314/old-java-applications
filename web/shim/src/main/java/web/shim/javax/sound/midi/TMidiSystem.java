package web.shim.javax.sound.midi;

public class TMidiSystem {
    private TMidiSystem() {
    }

    public static TSynthesizer getSynthesizer() throws TMidiUnavailableException {
        return new TWebSynthesizer();
    }
}
