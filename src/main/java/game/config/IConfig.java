package game.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

public interface IConfig extends Serializable {
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class Pack implements IConfig {
        private String urlPrefix;
        private String infoFile;
        private Asset[] assets;

        @Data
        public static class Asset implements Serializable {
            private int id;
            private String name;
            private String file;
            private int v;
            private String s;
        }
    }

}
