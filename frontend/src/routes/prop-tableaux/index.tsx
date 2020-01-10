import { Fragment, h } from "preact";

import Switch from "../../components/switch";

import { useState } from "preact/hooks";
import HintIcon, { Hint } from "../../components/hint";
import ClauseInput from "../../components/input/clause";
import Format from "../../components/input/clause/format";
import Radio from "../../components/radio";
import { useAppState } from "../../helpers/app-state";
import {
    CnfStrategy,
    TableauxParams,
    TableauxType
} from "../../types/tableaux";

interface Props {}

const Tableaux: preact.FunctionalComponent<Props> = () => {
    const { smallScreen } = useAppState();

    const [tabType, setTabType] = useState(TableauxType.unconnected);
    const [regular, setRegular] = useState(false);
    const [backtracking, setBacktracking] = useState(false);
    const [cnfStrategy, setStrategy] = useState(CnfStrategy.optimal);

    /**
     * Handle the selection of a TableauxType
     * @param {Event} e - The event to handle
     * @returns {void}
     */
    const handleTabTypeSelect = (e: Event) => {
        const target = e.target as HTMLInputElement;

        setTabType(target.id as TableauxType);
    };

    /**
     * Handle force naive strategy switch setting
     * @param {boolean} forceNaive - Switch setting (false: optimal, true: naive)
     * @returns {void}
     */
    const strategySelect = (forceNaive: boolean) => {
        setStrategy(forceNaive ? CnfStrategy.naive : CnfStrategy.optimal);
    };

    const params: TableauxParams = {
        type: tabType,
        regular,
        backtracking,
        cnfStrategy
    };

    return (
        <Fragment>
            <ClauseInput calculus="prop-tableaux" params={params} />
            <div class="card">
                <h3>Parameters</h3>
                <Hint top={smallScreen} />
                <div class="flex-container">
                    <div class="radios">
                        <Radio
                            id={TableauxType.unconnected}
                            group="connected"
                            label="Unconnected"
                            checked={tabType === TableauxType.unconnected}
                            onSelect={handleTabTypeSelect}
                        />
                        <HintIcon hint="An unconnected tableaux poses no restrictions as to when a clause can be expanded" />
                        <br />
                        <Radio
                            id={TableauxType.weak}
                            group="connected"
                            label="Weakly Connected"
                            checked={tabType === TableauxType.weak}
                            onSelect={handleTabTypeSelect}
                        />
                        <HintIcon hint="A weakly connected tableaux enforces that every inner node in the proof tree has at least one closed leaf as a child" />
                        <br />
                        <Radio
                            id={TableauxType.strong}
                            group="connected"
                            label="Strongly Connected"
                            checked={tabType === TableauxType.strong}
                            onSelect={handleTabTypeSelect}
                        />
                        <HintIcon hint="A strongly connected tableaux enforces that every inner node in the proof tree has at least one child that is closed with its parent node" />
                    </div>
                    <div class="switches">
                        <Switch label="Regular" onChange={setRegular} />
                        <HintIcon hint="A regular tableaux does not allow duplicate literals on any branch in the proof tree" />
                        <br />

                        <Switch
                            label="Backtracking"
                            onChange={setBacktracking}
                        />
                        <HintIcon hint="Enables ability to undo moves" />
                        <br />
                        <Switch
                            label="Naive CNF transformation"
                            onChange={strategySelect}
                        />
                        <HintIcon hint="New variables may be introduced when converting a formula to CNF for efficiency. Enable this to enforce the naive transformation without extra variables." />
                    </div>
                </div>
            </div>
            <Format />
        </Fragment>
    );
};

export default Tableaux;
