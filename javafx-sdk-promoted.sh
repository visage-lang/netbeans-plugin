#########################################################################
# This script is purposed to set correct URL of JavaFX SDK
#
#  (c) Martin Ryzl, SUN Microsystems, Oct 2008
#########################################################################

#!/bin/bash
echo "Promoted JavaFX SDK build b42 (749)"
export SDK_URL=http://getjfx.sfbay.sun.com/hudson/job/javafx1.0/749/
sh -x $WORKSPACE/main/contrib/javafx-nb-plugin-build.sh
