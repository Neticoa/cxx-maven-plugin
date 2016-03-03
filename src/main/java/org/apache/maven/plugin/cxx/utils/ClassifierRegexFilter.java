package org.apache.maven.plugin.cxx.utils;

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
 
import org.apache.maven.artifact.Artifact;
import org.apache.maven.shared.artifact.filter.collection.AbstractArtifactFeatureFilter;
 
/**
 * Our personnal dependencies classifier filter with regexp capabilities
 */
public class ClassifierRegexFilter
extends AbstractArtifactFeatureFilter
{
    /**
     * @param include comma separated list with includes.
     * @param exclude comma separated list with excludes.
     */
    public ClassifierRegexFilter( String include, String exclude )
    {
        super( include, exclude );
    }

    /** {@inheritDoc} */
    protected String getArtifactFeature( Artifact artifact )
    {
        return artifact.getClassifier();
    }
    
    /**
     * Allows Feature comparison to be customized
     * 
     * @param lhs String artifact's feature
     * @param rhs String feature from exclude or include list
     * @return boolean true if features match
     */
    protected boolean compareFeatures( String lhs, String rhs )
    {
        //getLog().debug( "check if '" + lhs + "' (artifact's classifier feature) Regex match '"
        //   + rhs + "' (exclude or include pattern)" );
        // If lhs is null, check that rhs is null. Otherwise check if strings are equal.
        return ( lhs == null ? rhs == null : lhs.matches( rhs ) /*lhs.equals( rhs )*/ );
    }
}
 
