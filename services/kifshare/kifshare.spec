%define __jar_repack %{nil}
%define debug_package %{nil}
%define __strip /bin/true
%define __os_install_post   /bin/true
%define __check_files /bin/true

Summary: kifshare
Name: kifshare
Version: 3.2.8
Release: 1
Epoch: 0
BuildArchitectures: noarch
Group: Applications
BuildRoot: %{_tmppath}/%{name}-%{version}-buildroot
License: BSD
Provides: kifshare
Requires: iplant-service-config >= 0.1.0-5
Requires: java-1.7.0-openjdk
Source0: %{name}-build.tar.gz

%description
iPlant Quickshare for iRODS

%pre
getent group iplant > /dev/null || groupadd -r iplant
getent passwd iplant > /dev/null || useradd -r -g iplant -md /home/iplant -s /bin/bash -c "User for the iplant services." iplant
exit 0

%prep
%setup -n %{name}-build
mkdir -p $RPM_BUILD_ROOT/etc/init.d/

%build
unset JAVA_OPTS
lein2 deps
lein2 compile
lein2 uberjar

%install
install -d $RPM_BUILD_ROOT/usr/local/lib/kifshare/
install -d $RPM_BUILD_ROOT/usr/local/lib/kifshare/resources/
install -d $RPM_BUILD_ROOT/usr/local/lib/kifshare/resources/public/
install -d $RPM_BUILD_ROOT/usr/local/lib/kifshare/resources/public/css/
install -d $RPM_BUILD_ROOT/usr/local/lib/kifshare/resources/public/flash/
install -d $RPM_BUILD_ROOT/usr/local/lib/kifshare/resources/public/js/
install -d $RPM_BUILD_ROOT/usr/local/lib/kifshare/resources/public/img/
install -d $RPM_BUILD_ROOT/var/run/kifshare/
install -d $RPM_BUILD_ROOT/var/lock/subsys/kifshare/
install -d $RPM_BUILD_ROOT/var/log/kifshare/
install -d $RPM_BUILD_ROOT/etc/kifshare/

install kifshare $RPM_BUILD_ROOT/etc/init.d/
install target/kifshare-3.2.8-standalone.jar $RPM_BUILD_ROOT/usr/local/lib/kifshare/
install conf/log4j.properties $RPM_BUILD_ROOT/etc/kifshare/
install build/public/robots.txt $RPM_BUILD_ROOT/usr/local/lib/kifshare/resources/public/
install build/public/css/960.css $RPM_BUILD_ROOT/usr/local/lib/kifshare/resources/public/css/
install build/public/css/jquery-ui-1.8.21.custom.css $RPM_BUILD_ROOT/usr/local/lib/kifshare/resources/public/css/
install build/public/css/kif.css $RPM_BUILD_ROOT/usr/local/lib/kifshare/resources/public/css/
install build/public/css/reset.css $RPM_BUILD_ROOT/usr/local/lib/kifshare/resources/public/css/
install build/public/flash/ZeroClipboard.swf $RPM_BUILD_ROOT/usr/local/lib/kifshare/resources/public/flash/
install build/public/img/iplant-logo-small.png $RPM_BUILD_ROOT/usr/local/lib/kifshare/resources/public/img/
install build/public/img/iplant_logo.ico $RPM_BUILD_ROOT/usr/local/lib/kifshare/resources/public/img/
install build/public/img/powered_by_iplant_logo.png $RPM_BUILD_ROOT/usr/local/lib/kifshare/resources/public/img/
install build/public/js/jquery.tooltip.min.js $RPM_BUILD_ROOT/usr/local/lib/kifshare/resources/public/js/
install build/public/js/jquery.zclip.min.js $RPM_BUILD_ROOT/usr/local/lib/kifshare/resources/public/js/
install build/public/js/json2.js $RPM_BUILD_ROOT/usr/local/lib/kifshare/resources/public/js/
install build/public/js/kif.js $RPM_BUILD_ROOT/usr/local/lib/kifshare/resources/public/js/

%post
/sbin/chkconfig --add kifshare

%preun
if [ $1 -eq 0 ] ; then
	/sbin/service kifshare stop >/dev/null 2>&1
	/sbin/chkconfig --del kifshare
fi

%postun
if [ "$1" -ge "1" ] ; then
	/sbin/service kifshare condrestart >/dev/null 2>&1 || :
fi

%clean
lein2 clean
rm -r $RPM_BUILD_ROOT

%files
%dir %attr(-,iplant,iplant) /usr/local/lib/kifshare/
%dir %attr(-,iplant,iplant) /usr/local/lib/kifshare/resources/
%dir %attr(-,iplant,iplant) /usr/local/lib/kifshare/resources/public/
%dir %attr(-,iplant,iplant) /usr/local/lib/kifshare/resources/public/css/
%dir %attr(-,iplant,iplant) /usr/local/lib/kifshare/resources/public/img/
%dir %attr(-,iplant,iplant) /usr/local/lib/kifshare/resources/public/js/
%dir %attr(-,iplant,iplant) /usr/local/lib/kifshare/resources/public/flash/
%attr(-,iplant,iplant) /var/run/kifshare/
%attr(-,iplant,iplant) /var/lock/subsys/kifshare/
%attr(-,iplant,iplant) /var/log/kifshare/
%dir %attr(-,iplant,iplant) /etc/kifshare/

%config %attr(0644,iplant,iplant) /etc/kifshare/log4j.properties

%attr(0755,root,root) /etc/init.d/kifshare
%attr(0644,iplant,iplant) /usr/local/lib/kifshare/kifshare-3.2.8-standalone.jar
%attr(0644,iplant,iplant) /usr/local/lib/kifshare/resources/public/robots.txt
%attr(0644,iplant,iplant) /usr/local/lib/kifshare/resources/public/img/iplant_logo.ico
%attr(0644,iplant,iplant) /usr/local/lib/kifshare/resources/public/img/iplant-logo-small.png
%attr(0644,iplant,iplant) /usr/local/lib/kifshare/resources/public/img/powered_by_iplant_logo.png
%attr(0644,iplant,iplant) /usr/local/lib/kifshare/resources/public/css/960.css
%attr(0644,iplant,iplant) /usr/local/lib/kifshare/resources/public/css/jquery-ui-1.8.21.custom.css
%attr(0644,iplant,iplant) /usr/local/lib/kifshare/resources/public/css/kif.css
%attr(0644,iplant,iplant) /usr/local/lib/kifshare/resources/public/css/reset.css
%attr(0644,iplant,iplant) /usr/local/lib/kifshare/resources/public/flash/ZeroClipboard.swf
%attr(0644,iplant,iplant) /usr/local/lib/kifshare/resources/public/js/jquery.tooltip.min.js
%attr(0644,iplant,iplant) /usr/local/lib/kifshare/resources/public/js/jquery.zclip.min.js
%attr(0644,iplant,iplant) /usr/local/lib/kifshare/resources/public/js/json2.js
%attr(0644,iplant,iplant) /usr/local/lib/kifshare/resources/public/js/kif.js
