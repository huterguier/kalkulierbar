import { Fragment, h } from "preact";
import { useState } from "preact/hooks";

import ExampleList from "../../components/input/example-list";
import FormulaInput from "../../components/input/formula";
import Format from "../../components/input/formula/format";
import Switch from "../../components/input/switch";
import { Calculus, PSCCalculusType } from "../../types/calculus";
import { PSCParams } from "../../types/calculus/psc";

interface Props {
    calculus: PSCCalculusType;
}

const PSC: preact.FunctionalComponent<Props> = ({ calculus }) => {
    const [showOnlyApplicableRules, setShowOnlyApplicableRules] = useState(
        false,
    );

    let params;
    const pscParams: PSCParams = {
        showOnlyApplicableRules,
    };
    params = pscParams;

    return (
        <Fragment>
            <Format
                foLogic={calculus === Calculus.fosc}
                allowClauses={false}
                allowSequences={true}
            />
            <FormulaInput
                calculus={calculus}
                params={params}
                foLogic={calculus === Calculus.fosc}
                sequentPlaceholder={true}
            />

            <div class="card">
                <h3>Parameters</h3>
                <div class="flex-container">
                    <div class="first">
                        <Switch
                            label="Show only applicable rules"
                            onChange={setShowOnlyApplicableRules}
                            initialState={false}
                        />
                    </div>
                </div>
            </div>
            <ExampleList calculus={calculus} />
        </Fragment>
    );
};

export default PSC;
