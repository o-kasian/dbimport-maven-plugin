function alert(message) {
    stdout.println(message);
}

function require(url) {
    if (url.indexOf("classpath:") !== -1) {
        jSEngine.importResource(utl.substr("classpath:".length));
        return;
    }
    jSEngine.importURL(url);
}

var console = {
    log: function(msg) {
        stdout.println(msg);
    },
    error: function(msg) {
        stderr.println(msg);
    },
    debug: function(msg) {
        stdout.println(msg);
    }
};