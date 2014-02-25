package com.wistron.youtubedmc;


class Meta {
    public String num;
    public String type;
    public String ext;

    Meta(String num, String ext, String type) {
        this.num = num;
        this.ext = ext;
        this.type = type;
    }
}

class Video {
    public String ext = "";
    public String type = "";
    public String url = "";

    Video(String ext, String type, String url) {
        this.ext = ext;
        this.type = type;
        this.url = url;
    }
}
