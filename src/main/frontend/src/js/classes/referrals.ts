import * as $ from 'jquery';

export default class Referrals {
    constructor() {
        let rewardsContainer = document.querySelector('#rewards-list-content');
        if (rewardsContainer != null) {
            this._getReferrals()
                .then((html) => {
                    rewardsContainer.innerHTML = html
                })
                .catch(function (ex) {
                    console.log('Something when wrong during getting referrals', ex);
                });
        }
    }

    private _getReferrals(): Promise<string> {
        return $.get('/referrals').promise();
    }
}
