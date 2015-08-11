/*global module:false*/
module.exports = function(grunt) {

  // Project configuration.
  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),
    meta: {
      version: '0.1.0',
      banner: '/*! PROJECT_NAME - v<%= meta.version %> - ' +
        '<%= grunt.template.today("yyyy-mm-dd") %>\n' +
        '* http://PROJECT_WEBSITE/\n' +
        '* Copyright (c) <%= grunt.template.today("yyyy") %> ' +
        'YOUR_NAME; Licensed MIT */'
    },

    jslint: {
      files: ['grunt.js', 'ui/src/js/kif.js', 'test/**/*.js'],
      directives : {
        predef: [
          'document',
          'jQuery',
          '$',
          'Mustache',
          '_',
          'ZeroClipboard',
          'zclip',
          'alert',
          'window'
        ]
      },
      options: {
        nomen: true
      }
    },
    qunit: {
      files: ['test/**/*.html']
    },
    concat: {
      dist: {
        src: ['<banner:meta.banner>', '<file_strip_banner:src/FILE_NAME.js>'],
        dest: 'dist/FILE_NAME.js'
      }
    },
    uglify: {
      dist: {
        files : {
            "build/public/js/kif.js" : ["ui/src/js/kif.js"]
        }
      }
    },
    copy: {
      main: {
        files: [
            {
              expand: true,
              flatten: true,
              cwd: 'ui/src/js/',
              src: ["jquery*.js", "json2.js", "ZeroClipboard*.js"],
              dest: "build/public/js/"
            },
            {
              expand: true,
              flatten: true,
              cwd: 'ui/src/css/',
              src: ['*.css'],
              dest: "build/public/css/"
            },
            {
              expand: true,
              flatten: true,
              cwd: 'ui/src/flash/',
              src: ['*'],
              dest: "build/public/flash/"
            },
            {
              expand: true,
              flatten: true,
              cwd: 'ui/src/img',
              src: ['*'],
              dest: "build/public/img/"
            },
            {
              expand: true,
              flatten: true,
              cwd: 'ui/src',
              src: ["robots.txt"],
              dest: "build/public/"
            }
        ]
      },
      kifjs: {
        files: [
          {src: ["ui/src/js/kif.js"], dest: "build/public/js/"}
        ]
      }
    },
    shell: {
      _options: {
        failOnError: true
      },
      make_js_resources: {
        command: 'mkdir -p build/public/js'
      },
      make_css_resources: {
        command: 'mkdir -p build/public/css'
      },
      make_flash_resources: {
        command: 'mkdir -p build/public/flash'
      },
      make_img_resources: {
        command: 'mkdir -p build/public/img'
      },
      lein_clean: {
        command: 'lein clean'
      },
      lein_deps: {
        command: 'lein deps'
      },
      lein_uberjar: {
        command: 'lein uberjar',
        stdout: true
      },
      clean_resources: {
        command: 'rm -rf build/'
      }
    },
    less: {
      build: {
        files: {
          "build/public/css/kif.css" : "ui/src/css/kif.less"
        }
      }
    },
    watch: {
      files: ["ui/src/js/*.js", "ui/src/css/*.less", "ui/src/img/*", "ui/src/flash/*"],
      tasks: ['build-resources-dev', 'copy:kifjs']
    },
    jshint: {
      options: {
        curly: true,
        eqeqeq: true,
        immed: true,
        latedef: true,
        newcap: true,
        noarg: true,
        sub: true,
        undef: true,
        boss: true,
        eqnull: true,
        browser: true
      },
      globals: {
        jQuery: true,
        $:true,
        Mustache: true,
        _: true,
        ZeroClipboard: true,
        zclip: true,
        alert: true
      }
    },
  });



  // Default task.
  grunt.loadNpmTasks('grunt-jslint');
  grunt.loadNpmTasks('grunt-contrib-less');
  grunt.loadNpmTasks('grunt-shell');
  grunt.loadNpmTasks('grunt-contrib-copy');
  grunt.loadNpmTasks('grunt-contrib-uglify');
  grunt.registerTask('default', ['jslint', 'qunit', 'concat', 'uglify']);
  //grunt.loadNpmTasks('watch');

  grunt.registerTask('make-resources', ['shell:make_js_resources', 'shell:make_css_resources', 'shell:make_flash_resources', 'shell:make_img_resources']);
  grunt.registerTask('build-resources', ['jslint', 'make-resources', 'less', 'copy', 'uglify']);
  grunt.registerTask('build-resources-dev', ['jslint', 'make-resources', 'less', 'copy']);
  grunt.registerTask('build-clj', ['shell:lein_clean', 'shell:lein_deps', 'shell:lein_uberjar']);
  grunt.registerTask('build-all', ['build-resources', 'build-clj']);
  grunt.registerTask('clean-all', ['shell:lein_clean', 'shell:clean_resources']);

};
