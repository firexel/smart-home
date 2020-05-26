package com.seraph.luxmeter.app

import com.seraph.luxmeter.presenter.ExperimentSettingsPresenter
import com.seraph.luxmeter.presenter.OngoingExperimentPresenter

interface PresenterFactory {
    fun newExperimentSettingsPresenter(view: ExperimentSettingsPresenter.View): ExperimentSettingsPresenter
    fun newOngoingExperimentPresenter(view: OngoingExperimentPresenter.View): OngoingExperimentPresenter
}