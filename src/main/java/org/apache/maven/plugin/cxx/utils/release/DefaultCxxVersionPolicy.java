package org.apache.maven.plugin.cxx.utils.release;

/*
 * Copyright (C) 2011-2016, Neticoa SAS France - Tous droits réservés.
 * Author(s) : Franck Bonin, Neticoa SAS France
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import org.apache.maven.shared.release.policy.PolicyException;
import org.apache.maven.shared.release.policy.version.VersionPolicyRequest;
import org.apache.maven.shared.release.policy.version.VersionPolicyResult;
import org.apache.maven.shared.release.versions.DefaultVersionInfo;
import org.apache.maven.shared.release.versions.VersionParseException;

/**
 * 
 * @author Franck Bonin
 * 
 * @plexus.component role="org.apache.maven.plugin.cxx.utils.release.CxxVersionPolicy" role-hint="default"
 */
public class DefaultCxxVersionPolicy
    implements CxxVersionPolicy
{

    public VersionPolicyResult getReleaseVersion( VersionPolicyRequest request )
        throws PolicyException, VersionParseException
    {
        String releaseVersion = new DefaultVersionInfo( request.getVersion() ).getReleaseVersionString();
        return new VersionPolicyResult().setVersion( releaseVersion );
    }
    
    public VersionPolicyResult getBranchVersion( VersionPolicyRequest request )
        throws PolicyException, VersionParseException
    {
        String branchVersion =
           new DefaultVersionInfo( request.getVersion() ).getNextVersion().getReleaseVersionString();
        return new VersionPolicyResult().setVersion( branchVersion );
    }
    
    public VersionPolicyResult getSnapshotVersion( VersionPolicyRequest request )
        throws PolicyException, VersionParseException
    {
        String snapshotVersion =
           new DefaultVersionInfo( request.getVersion() ).getSnapshotVersionString();
        return new VersionPolicyResult().setVersion( snapshotVersion );
    }

    public VersionPolicyResult getDevelopmentVersion( VersionPolicyRequest request )
        throws PolicyException, VersionParseException
    {
        String developmentVersion =
            new DefaultVersionInfo( request.getVersion() ).getNextVersion().getSnapshotVersionString();
        return new VersionPolicyResult().setVersion( developmentVersion );
    }

}
