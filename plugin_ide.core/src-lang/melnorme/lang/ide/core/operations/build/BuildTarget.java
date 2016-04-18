/*******************************************************************************
 * Copyright (c) 2015 Bruno Medeiros and other Contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bruno Medeiros - initial API and implementation
 *******************************************************************************/
package melnorme.lang.ide.core.operations.build;

import static melnorme.utilbox.core.Assert.AssertNamespace.assertNotNull;
import static melnorme.utilbox.core.CoreUtil.option;

import org.eclipse.core.resources.IProject;

import melnorme.lang.ide.core.LangCore;
import melnorme.lang.ide.core.launch.LaunchMessages;
import melnorme.lang.ide.core.operations.ILangOperationsListener_Default.IOperationMonitor;
import melnorme.lang.ide.core.operations.ToolManager;
import melnorme.lang.ide.core.operations.build.BuildManager.BuildType;
import melnorme.lang.ide.core.operations.build.BuildTargetOperation.BuildOperationParameters;
import melnorme.lang.ide.core.utils.ResourceUtils;
import melnorme.lang.tooling.bundle.BuildConfiguration;
import melnorme.lang.tooling.bundle.BundleInfo;
import melnorme.lang.tooling.bundle.LaunchArtifact;
import melnorme.lang.tooling.data.AbstractValidator;
import melnorme.lang.tooling.data.Severity;
import melnorme.lang.tooling.data.StatusException;
import melnorme.lang.tooling.ops.util.ValidationMessages;
import melnorme.utilbox.collections.Indexable;
import melnorme.utilbox.core.CommonException;
import melnorme.utilbox.misc.Location;

public class BuildTarget extends AbstractValidator {
	
	public final BuildManager buildMgr = LangCore.getBuildManager();
	
	protected final IProject project;
	protected final BundleInfo bundleInfo;
	protected final BuildTargetDataView targetData;
	protected final BuildType buildType;
	protected final BuildConfiguration buildConfiguration;
	
	public static BuildTarget create(IProject project, BundleInfo bundleInfo, BuildTargetDataView buildTargetData,
			BuildType buildType, String buildConfigName) throws CommonException {
		BuildConfiguration buildConfig = buildType.getValidBuildconfiguration(buildConfigName, bundleInfo);
		return new BuildTarget(project, bundleInfo, buildTargetData, buildType, buildConfig);
	}
	
	public BuildTarget(IProject project, BundleInfo bundleInfo, 
			BuildTargetDataView buildTargetData, BuildType buildType, 
			BuildConfiguration buildConfig) {
		this.project = assertNotNull(project);
		this.bundleInfo = assertNotNull(bundleInfo);
		
		this.targetData = assertNotNull(buildTargetData.copy());
		assertNotNull(buildTargetData.getTargetName());
		this.buildType = assertNotNull(buildType);
		
		this.buildConfiguration = assertNotNull(buildConfig);
	}
	
	public BuildManager getBuildManager() {
		return buildMgr;
	}
	
	public IProject getProject() {
		return project;
	}
	
	@Override
	public String toString() {
		return project.getName() + "//" + targetData.getTargetName();
	}
	
	public Location getProjectLocation() throws CommonException {
		return ResourceUtils.getProjectLocation2(project);
	}
	
	public String getBuildTargetName() {
		return assertNotNull(targetData.getTargetName());
	}
	
	public String getTargetName() {
		return assertNotNull(targetData.getTargetName());
	}
	
	public boolean isNormalBuildEnabled() {
		return targetData.isNormalBuildEnabled();
	}
	
	public boolean isAutoBuildEnabled() {
		return targetData.isAutoBuildEnabled();
	}
	
	public BuildTargetDataView getData() {
		return targetData;
	}
	
	public BuildTargetData getDataCopy() {
		return targetData.copy();
	}
	
	public String getBuildConfigName() {
		return buildConfiguration.getName();
	}
	
	public String getBuildTypeName() {
		return buildType.getName();
	}
	
	public BuildConfiguration getBuildConfiguration() {
		return buildConfiguration;
	}
	
	public BuildType getBuildType() {
		return buildType;
	}
	
	public boolean isDefaultBuildType() {
		return getBuildType() == buildMgr.getDefaultBuildType();
	}
	
	public BundleInfo getBundleInfo() {
		return bundleInfo;
	}
	
	public BuildTarget getDerivedBuildTarget(BuildTargetData data) {
		return new BuildTarget(
			getProject(), 
			getBundleInfo(), 
			data, 
			getBuildType(), 
			getBuildConfiguration()
		);
	}
	
	/* -----------------  ----------------- */
	
	public String getDefaultBuildCommand() throws StatusException {
		try {
			return getBuildType().getDefaultCommandLine(this);
		} catch(CommonException e) {
			throw e.toStatusException();
		}
	}
	
	public String getEffectiveBuildCommand() throws StatusException {
		String buildOptions = targetData.getBuildArguments();
		if(buildOptions != null) {
			return buildOptions;
		}
		return getDefaultBuildCommand();
	}
	
	/* -----------------  ----------------- */
	
	public String getEffectiveValidExecutablePath() throws StatusException {
		String executablePath = targetData.getExecutablePath();
		if(executablePath != null) {
			return executablePath;
		}
		
		return getDefaultExecutablePath();
	}
	
	public String getDefaultExecutablePath() throws StatusException {
		LaunchArtifact mainLaunchArtifact = getMainLaunchArtifact();
		if(mainLaunchArtifact == null) {
			throw new StatusException(LaunchMessages.MSG_BuildTarget_NoExecutableAvailable());
		}
		return mainLaunchArtifact.getArtifactPath();
	}
	
	public Indexable<LaunchArtifact> getSubLaunchArtifacts() throws CommonException {
		return getBuildType().getSubTargetLaunchArtifacts(this);
	}
	
	public LaunchArtifact getMainLaunchArtifact() throws StatusException {
		try {
			return getBuildType().getMainLaunchArtifact(this);
		} catch(CommonException e) {
			throw e.toStatusException();
		}
	}
	
	public Location getValidExecutableLocation() throws StatusException {
		return getValidExecutableLocation(getEffectiveValidExecutablePath());
	}
	
	public Location getValidExecutableLocation(String exeFilePathString) throws StatusException {
		if(exeFilePathString == null || exeFilePathString.isEmpty()) {
			throw new StatusException(LaunchMessages.BuildTarget_NoArtifactPathSpecified);
		}
		
		Location exeFileLocation;
		try {
			exeFileLocation = Location.create(getProjectLocation(), exeFilePathString);
		} catch(CommonException e) {
			throw e.toStatusException();
		}
		
		if(exeFileLocation.toFile().exists() && !exeFileLocation.toFile().isFile()) {
			throw new StatusException(Severity.ERROR, ValidationMessages.Location_NotAFile(exeFileLocation));
		}
		return exeFileLocation;
	}
	
	/* -----------------  ----------------- */
	
	public void validateForBuild(ToolManager toolManager) throws StatusException {
		getCommandInvocation(toolManager).validate();
		getValidExecutableLocation(); // TODO: Build Target Editor validate this
	}
	
	public CommandInvocation getCommandInvocation(ToolManager toolManager) throws StatusException {
		String buildCommandString = getEffectiveBuildCommand();
		VariablesResolver variablesResolver = toolManager.getVariablesManager(option(getProject()));
		return new BuildCommandInvocation(buildCommandString, variablesResolver);
	}
	
	public static class BuildCommandInvocation extends CommandInvocation {
		public BuildCommandInvocation(String commandArguments, VariablesResolver variablesResolver) {
			super(commandArguments, variablesResolver);
		}
		
		@Override
		protected void handleEmptyCommandLine() throws CommonException {
			throw new CommonException("Build command is empty.");
		}
	}
	
	public BuildTargetOperation getBuildOperation(ToolManager toolManager, IOperationMonitor opMonitor)
			throws CommonException {
		assertNotNull(opMonitor);
		
		CommandInvocation buildCommand = this.getCommandInvocation(toolManager);
		
		return getBuildType().getBuildOperation(new BuildOperationParameters(opMonitor, 
			toolManager, this.getProject(), this.getBuildTargetName(), buildCommand));
	}
	
}