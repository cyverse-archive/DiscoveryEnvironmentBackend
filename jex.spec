%define __jar_repack %{nil}
%define debug_package %{nil}
%define __strip /bin/true
%define __os_install_post   /bin/true
%define __check_files /bin/true
Summary: jex
Name: jex
Version: 0.1.0
Release: 15
Epoch: 0
BuildArchitectures: noarch
Group: Applications
BuildRoot: %{_tmppath}/%{name}-%{version}-buildroot
License: BSD
Provides: jex
Requires: iplant-service-config >= 0.1.0-5
Source0: %{name}-%{version}.tar.gz

%description
iPlant JEX

%pre
getent group condor > /dev/null || groupadd -r condor
getent passwd condor > /dev/null || useradd -r -g condor -d /home/iplant -s /bin/bash -c "User for the iPlant services." iplant
exit 0

%prep
%setup -q
mkdir -p $RPM_BUILD_ROOT/etc/init.d/

%build
unset JAVA_OPTS
lein deps
lein uberjar

%install
install -d $RPM_BUILD_ROOT/usr/local/lib/jex/
install -d $RPM_BUILD_ROOT/var/run/jex/
install -d $RPM_BUILD_ROOT/var/lock/subsys/jex/
install -d $RPM_BUILD_ROOT/var/log/jex/
install -d $RPM_BUILD_ROOT/etc/jex/

install jex $RPM_BUILD_ROOT/etc/init.d/
install jex-1.0.0-SNAPSHOT-standalone.jar $RPM_BUILD_ROOT/usr/local/lib/jex/
install conf/log4j.properties $RPM_BUILD_ROOT/etc/jex/

%post
/sbin/chkconfig --add jex

%preun
if [ $1 -eq 0 ] ; then
	/sbin/service jex stop >/dev/null 2>&1
	/sbin/chkconfig --del jex
fi

%postun
if [ "$1" -ge "1" ] ; then
	/sbin/service jex condrestart >/dev/null 2>&1 || :
fi

%clean
lein clean
rm -r lib/*
rm -r $RPM_BUILD_ROOT

%files
%attr(-,condor,condor) /usr/local/lib/jex/
%attr(-,condor,condor) /var/run/jex/
%attr(-,condor,condor) /var/lock/subsys/jex/
%attr(-,condor,condor) /var/log/jex/
%attr(-,condor,condor) /etc/jex/

%config %attr(0644,condor,condor) /etc/jex/log4j.properties

%attr(0755,root,root) /etc/init.d/jex
%attr(0644,condor,condor) /usr/local/lib/jex/jex-1.0.0-SNAPSHOT-standalone.jar


